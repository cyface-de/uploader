/*
 * Copyright 2023 Cyface GmbH
 *
 * This file is part of the Cyface Uploader.
 *
 * The Cyface Uploader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Cyface Uploader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Cyface Uploader. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.uploader

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.json.JsonHttpContent
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import de.cyface.model.RequestMetaData
import de.cyface.uploader.exception.AccountNotActivated
import de.cyface.uploader.exception.BadRequestException
import de.cyface.uploader.exception.ConflictException
import de.cyface.uploader.exception.EntityNotParsableException
import de.cyface.uploader.exception.ForbiddenException
import de.cyface.uploader.exception.InternalServerErrorException
import de.cyface.uploader.exception.MeasurementTooLarge
import de.cyface.uploader.exception.NetworkUnavailableException
import de.cyface.uploader.exception.ServerUnavailableException
import de.cyface.uploader.exception.SynchronisationException
import de.cyface.uploader.exception.SynchronizationInterruptedException
import de.cyface.uploader.exception.TooManyRequestsException
import de.cyface.uploader.exception.UnauthorizedException
import de.cyface.uploader.exception.UnexpectedResponseCode
import de.cyface.uploader.exception.UploadFailed
import de.cyface.uploader.exception.UploadSessionExpired
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.SSLException

/**
 * Implementation of the [Uploader].
 *
 * To use this interface just call [DefaultUploader.upload] with an authentication token, e.g. from
 * [DefaultAuthenticator.authenticate].
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 * @property apiEndpoint An API endpoint running a Cyface data collector service, like `https://some.url/api/v3`
 */
class DefaultUploader(private val apiEndpoint: String) : Uploader {

    @Suppress("unused", "CyclomaticComplexMethod", "LongMethod")
    override // Part of the API
    fun upload(
        jwtToken: String,
        metaData: RequestMetaData,
        file: File,
        progressListener: UploadProgressListener
    ): Result {
        return try {
            val jwtBearer = "Bearer $jwtToken"

            // Uploader
            val mediaContent =
                InputStreamContent("application/octet-stream", BufferedInputStream(FileInputStream(file)))
            mediaContent.length = file.length()
            LOGGER.debug("mediaContent.length: ${mediaContent.length}")
            val transport = NetHttpTransport() // Use Builder to modify behaviour
            val httpRequestInitializer = RequestInitializeHandler(metaData, jwtBearer)
            val uploader = MediaHttpUploader(mediaContent, transport, httpRequestInitializer)

            // We currently cannot merge multiple upload-chunk requests into one file on server side.
            // Thus, we prevent slicing the file into multiple files by increasing the chunk size.
            // If the file is larger sync would be successful but only the 1st chunk received DAT-730.
            // i.e. we throw an exception (which skips the upload) for too large measurements (44h+).
            uploader.chunkSize = MAX_CHUNK_SIZE
            if (file.length() > MAX_CHUNK_SIZE) {
                throw MeasurementTooLarge("Transfer file is too large: ${file.length()}")
            }

            // Add meta data to PreRequest
            val jsonFactory = GsonFactory()
            val preRequestBody = preRequestBody(metaData)
            uploader.metadata = JsonHttpContent(jsonFactory, preRequestBody)

            // Vert.X currently only supports compressing "down-stream" out of the box
            uploader.disableGZipContent = true

            // Progress
            uploader.progressListener = ProgressHandler(progressListener)

            // Upload
            val requestUrl = GenericUrl(endpoint())
            val response = uploader.upload(requestUrl)
            try {
                readResponse(response, jsonFactory)
            } finally {
                response.disconnect()
            }
        }

        // Soft catch errors in `UploadFailed` exception so that the caller can handle this without crashing.
        // This way the SDK's `SyncPerformer` can determine if the sync should be repeated.
        catch (e: SocketTimeoutException) {
            // Happened on emulator when endpoint is local network instead of 10.0.2.2 [DAT-727]
            // Server not reachable. Try again later.
            throw UploadFailed(ServerUnavailableException(e))
        } catch (e: SSLException) {
            LOGGER.warn("Caught SSLException: ${e.message}")
            // Thrown by OkHttp when the network is no longer available [DAT-740]. Try again later.
            val message = e.message
            if (message != null && message.contains("I/O error during system call, Broken pipe")) {
                throw UploadFailed(NetworkUnavailableException("Network became unavailable during upload."))
            }
            throw UploadFailed(SynchronisationException(e))
        } catch (e: InterruptedIOException) {
            LOGGER.warn("Caught InterruptedIOException: ${e.message}")
            val message = e.message
            if (message != null && message.contains("thread interrupted")) {
                // Request interrupted [DAT-741]. Try again later.
                throw UploadFailed(NetworkUnavailableException("Network interrupted during upload", e))
            }
            // InterruptedIOException while reading the response. Try again later.
            throw UploadFailed(SynchronisationException(e))
        } catch (e: IOException) {
            LOGGER.warn("Caught IOException: ${e.message}")
            val message = e.message
            if (message != null && message.contains("unexpected end of stream")) {
                // Unstable Wi-Fi connection [DAT-742]. transmission stream ended too early, likely because the sync
                // thread was interrupted (sync canceled). Try again later.
                throw SynchronizationInterruptedException("Upload interrupted", e)
            }
            // IOException while reading the response. Try again later.
            throw UploadFailed(SynchronisationException(e))
        } catch (e: MeasurementTooLarge) {
            // File is too large to be uploaded. Handle in caller (e.g. skip the upload).
            // The max size is currently static and set to 100 MB which should be about 44 hours of 100 Hz measurement.
            throw UploadFailed(e)
        } catch (e: BadRequestException) {
            throw UploadFailed(e) // `HTTP_BAD_REQUEST` (400).
        } catch (e: UnauthorizedException) {
            throw UploadFailed(e) // `HTTP_UNAUTHORIZED` (401).
        } catch (e: ForbiddenException) {
            // `HTTP_FORBIDDEN` (403). Seems to happen when server is unavailable. Handle in caller.
            throw UploadFailed(e)
        } catch (e: ConflictException) {
            throw UploadFailed(e) // `HTTP_CONFLICT` (409). Already uploaded. Handle in caller (e.g. mark as synced).
        } catch (e: EntityNotParsableException) {
            throw UploadFailed(e) // `HTTP_ENTITY_NOT_PROCESSABLE` (422).
        } catch (e: InternalServerErrorException) {
            throw UploadFailed(e) // `HTTP_INTERNAL_ERROR` (500).
        } catch (e: TooManyRequestsException) {
            throw UploadFailed(e) // `HTTP_TOO_MANY_REQUESTS` (429). Try again later.
        } catch (e: SynchronisationException) {
            throw UploadFailed(e) // IOException while reading the response. Try again later.
        } catch (e: UploadSessionExpired) {
            throw UploadFailed(e) // `HTTP_NOT_FOUND` (404). Try again.
        } catch (e: UnexpectedResponseCode) {
            throw UploadFailed(e) // Unexpected response code. Should be reported to the server admin.
        } catch (e: AccountNotActivated) {
            // `PRECONDITION_REQUIRED` (428). Shouldn't happen during upload, report to server admin.
            throw UploadFailed(e)
        }

        // Crash unexpected errors hard
        catch (e: MalformedURLException) {
            error(e) // The endpoint url is malformed.
        }
        // This is not yet thrown as a specific exception.
        /*catch (e: HostUnresolvable) {
            throw LoginFailed(e) // Network without internet connection. Try again later.
        }*/
    }

    override fun endpoint(): URL {
        return URL(DefaultAuthenticator.returnUrlWithTrailingSlash(apiEndpoint) + "measurements")
    }

    @Throws(
        BadRequestException::class,
        UnauthorizedException::class,
        ForbiddenException::class,
        ConflictException::class,
        EntityNotParsableException::class,
        InternalServerErrorException::class,
        TooManyRequestsException::class,
        SynchronisationException::class,
        UploadSessionExpired::class,
        UnexpectedResponseCode::class,
        AccountNotActivated::class
    )
    private fun readResponse(response: com.google.api.client.http.HttpResponse, jsonFactory: JsonFactory): Result {
        // Read response from connection
        val responseCode = response.statusCode
        val responseMessage = response.statusMessage
        val responseBody: String
        return try {
            responseBody = readResponseBody(response, jsonFactory)
            handleSuccess(HttpResponse(responseCode, responseBody, responseMessage))
        } catch (e: GoogleJsonResponseException) {
            val details = e.details
            if (details != null) {
                handleError(HttpResponse(responseCode, details.message, responseMessage))
            } else {
                handleError(HttpResponse(responseCode, e.toString(), responseMessage))
            }
            // Our server should only add JSON bodies to error responses or else there is no
            // way to read the error body with the Google API client library
        }
    }

    /**
     * Reads the body from the `com.google.api.client.http.HttpResponse`.
     *
     * This contains either the error or the success message.
     *
     * @param response the `HttpResponse` to read the body from
     * @param jsonFactory the `Factory` to be used to parse the response if it's JSON
     * @return the [HttpResponse] body
     * @throws GoogleJsonResponseException if the response does not contain a success status code
     * @throws SynchronisationException if an `IOException` occurred while reading the response
     */
    @Throws(GoogleJsonResponseException::class, SynchronisationException::class)
    private fun readResponseBody(
        response: com.google.api.client.http.HttpResponse,
        jsonFactory: JsonFactory
    ): String {
        // See `uploader.upload`: Handle error parsing correctly
        if (!response.isSuccessStatusCode) {
            throw GoogleJsonResponseException.from(jsonFactory, response)
        }

        // Read success response body
        try {
            response.content.use { inputStream -> return readInputStream(inputStream) }
        } catch (e: IOException) {
            // No errors, connection is not connected or server sent no useful data.
            // Unsure if this happens with the new 2021 protocol, thus, throwing an exception.
            throw SynchronisationException(e)
        }
    }

    internal class ProgressHandler(progressListener: UploadProgressListener) :
        MediaHttpUploaderProgressListener {

        private val progressListener: UploadProgressListener

        init {
            this.progressListener = progressListener
        }

        @Throws(IOException::class)
        override fun progressChanged(uploader: MediaHttpUploader) {
            LOGGER.debug("progress: ${uploader.progress}, uploaded: ${uploader.numBytesUploaded} Bytes")
            progressListener.updatedProgress(uploader.progress.toFloat())
        }

        companion object {

            /**
             * The logger used to log messages from this class. Configure it using
             * <tt>src/main/resources/logback.xml</tt>.
             */
            private val LOGGER = LoggerFactory.getLogger(ProgressHandler::class.java)
        }
    }

    companion object {
        /**
         * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
         */
        private val LOGGER = LoggerFactory.getLogger(DefaultUploader::class.java)

        /**
         * The charset used to parse Strings (e.g. for JSON data)
         */
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        val DEFAULT_CHARSET = Charsets.UTF_8

        /**
         * The status code returned when the MultiPart request is erroneous, e.g. when there is not exactly onf file
         * or a syntax error.
         */
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        const val HTTP_ENTITY_NOT_PROCESSABLE = 422

        /**
         * The status code returned when the server responded that the user account is not activated.
         */
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        const val ACCOUNT_NOT_ACTIVATED = 428

        /**
         * The status code returned when the server thinks that this client sent too many requests in to short time.
         * This helps to prevent DDoS attacks. The client should just retry a short time later.
         */
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        const val HTTP_TOO_MANY_REQUESTS = 429

        private const val MB_FROM_MEDIA_HTTP_UPLOADER = 0x100000

        /**
         * With a sensor frequency of 100 Hz this supports Measurements up to ~ 44 hours.
         */
        private const val MAX_CHUNK_SIZE = 100 * MB_FROM_MEDIA_HTTP_UPLOADER

        /**
         * Http code which indicates that the upload intended by the client should be skipped.
         *
         * The server is not interested in the data, e.g. or missing location data or data from a location of no
         * interest.
         */
        private const val SKIP_UPLOAD = 412

        /**
         * Http code which indicates that one of the upload requests contained a too large payload.
         *
         * This should not happen as the client checks the file size and skips too large measurement
         * and the pre-request only contains metadata and, thus, should be very small (< 1 KB).
         */
        private const val PAYLOAD_TOO_LARGE = 413

        /**
         * Assembles a `HttpContent` object which contains the metadata.
         *
         * @param metaData The metadata to convert.
         * @return The meta data as `HttpContent`.
         */
        fun preRequestBody(metaData: RequestMetaData): Map<String, String> {
            val attributes: MutableMap<String, String> = HashMap()

            // Location meta data
            if (metaData.startLocation != null) {
                attributes["startLocLat"] = metaData.startLocation.latitude.toString()
                attributes["startLocLon"] = metaData.startLocation.longitude.toString()
                attributes["startLocTS"] = metaData.startLocation.timestamp.toString()
            }
            if (metaData.endLocation != null) {
                attributes["endLocLat"] = metaData.endLocation.latitude.toString()
                attributes["endLocLon"] = metaData.endLocation.longitude.toString()
                attributes["endLocTS"] = metaData.endLocation.timestamp.toString()
            }
            attributes["locationCount"] = metaData.locationCount.toString()

            // Remaining meta data
            attributes["deviceId"] = metaData.deviceIdentifier
            attributes["measurementId"] = metaData.measurementIdentifier
            attributes["deviceType"] = metaData.deviceType
            attributes["osVersion"] = metaData.operatingSystemVersion
            attributes["appVersion"] = metaData.applicationVersion
            attributes["length"] = metaData.length.toString()
            attributes["modality"] = metaData.modality.toString()
            attributes["formatVersion"] = metaData.formatVersion.toString()
            attributes["logCount"] = metaData.logCount.toString()
            attributes["imageCount"] = metaData.imageCount.toString()
            attributes["videoCount"] = metaData.videoCount.toString()
            return attributes
        }

        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        @JvmStatic
        fun handleSuccess(response: HttpResponse): Result {
            // Handle known success responses
            when (response.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    LOGGER.debug("200: Login successful")
                    return Result.LOGIN_SUCCESSFUL
                }

                HttpURLConnection.HTTP_CREATED -> {
                    LOGGER.debug("201: Upload successful")
                    return Result.UPLOAD_SUCCESSFUL
                }
            }
            error("Unknown success code: ${response.responseCode}")
        }

        @Throws(
            BadRequestException::class,
            UnauthorizedException::class,
            ForbiddenException::class,
            ConflictException::class,
            EntityNotParsableException::class,
            InternalServerErrorException::class,
            TooManyRequestsException::class,
            UploadSessionExpired::class,
            UnexpectedResponseCode::class,
            AccountNotActivated::class
        )
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        @JvmStatic
        fun handleError(response: HttpResponse): Result {
            // Handle known error responses
            return when (response.responseCode) {
                HttpURLConnection.HTTP_BAD_REQUEST -> {
                    LOGGER.warn("400: Unknown error")
                    throw BadRequestException(response.body)
                }

                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    LOGGER.warn("401: Bad credentials or missing authorization information")
                    throw UnauthorizedException(response.body)
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    LOGGER.warn("403: The authorized user has no permissions to post measurements")
                    throw ForbiddenException(response.body)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // This code is thrown if the upload is expired. Client should restart upload.
                    LOGGER.warn("404: Did the upload session expire? Try again.")
                    throw UploadSessionExpired(response.body)
                }

                HttpURLConnection.HTTP_CONFLICT -> {
                    LOGGER.warn("409: The measurement already exists on the server.")
                    throw ConflictException(response.body)
                }

                SKIP_UPLOAD -> {
                    LOGGER.warn("412: Skip upload")
                    Result.UPLOAD_SKIPPED
                }

                PAYLOAD_TOO_LARGE -> {
                    LOGGER.warn("413: Payload too large")
                    error(response.body)
                }

                HTTP_ENTITY_NOT_PROCESSABLE -> {
                    LOGGER.warn("422: Multipart request is erroneous.")
                    throw EntityNotParsableException(response.body)
                }

                ACCOUNT_NOT_ACTIVATED -> {
                    LOGGER.warn("428: User account not activated.")
                    throw AccountNotActivated(response.body)
                }

                HTTP_TOO_MANY_REQUESTS -> {
                    LOGGER.warn("429: Server reported too many requests received from this client.")
                    throw TooManyRequestsException(response.body)
                }

                HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    LOGGER.warn("500: Server reported internal error.")
                    throw InternalServerErrorException(response.body)
                }

                else -> {
                    LOGGER.error("${response.responseCode}: Server reported with an unexpected error code.")
                    throw UnexpectedResponseCode(response.body)
                }
            }
        }

        /**
         * Extracts the String from the provided `InputStream`.
         *
         * @param inputStream the `InputStream` to read from
         * @return the [String] read from the InputStream. If an I/O error occurs while reading from the stream, the
         * already read string is returned which might my empty or cut short.
         */
        @Suppress("MemberVisibilityCanBePrivate", "NestedBlockDepth") // Part of the API
        @JvmStatic
        fun readInputStream(inputStream: InputStream): String {
            try {
                try {
                    BufferedReader(
                        InputStreamReader(inputStream, DEFAULT_CHARSET)
                    ).use { bufferedReader ->
                        val responseString = StringBuilder()
                        var line: String?
                        while (bufferedReader.readLine().also { line = it } != null) {
                            responseString.append(line)
                        }
                        return responseString.toString()
                    }
                } catch (e: UnsupportedEncodingException) {
                    error(e)
                }
            } catch (e: IOException) {
                error(e)
            }
        }
    }
}
