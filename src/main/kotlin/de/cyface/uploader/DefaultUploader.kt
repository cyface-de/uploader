/*
 * Copyright 2023-2024 Cyface GmbH
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
import de.cyface.uploader.model.Attachment
import de.cyface.uploader.model.Measurement
import de.cyface.uploader.model.Uploadable
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
 * To use this interface just call [DefaultUploader.uploadMeasurement] or [DefaultUploader.uploadAttachment].
 *
 * @author Armin Schnabel
 * @property apiEndpoint An API endpoint running a Cyface data collector service, like `https://some.url/api/v3`
 */
@SuppressWarnings("TooManyFunctions")
class DefaultUploader(private val apiEndpoint: String) : Uploader {

    @Suppress("unused", "CyclomaticComplexMethod", "LongMethod") // Part of the API
    override fun uploadMeasurement(
        jwtToken: String,
        uploadable: Measurement,
        file: File,
        progressListener: UploadProgressListener
    ): Result {
        val endpoint = measurementsEndpoint(uploadable)
        return uploadFile(jwtToken, uploadable, file, endpoint, progressListener)
    }

    override fun uploadAttachment(
        jwtToken: String,
        uploadable: Attachment,
        file: File,
        fileName: String,
        progressListener: UploadProgressListener,
    ): Result {
        val endpoint = attachmentsEndpoint(uploadable)
        return uploadFile(jwtToken, uploadable, file, endpoint, progressListener)
    }

    override fun measurementsEndpoint(uploadable: Uploadable): URL {
        return URL(returnUrlWithTrailingSlash(apiEndpoint) + "measurements")
    }

    override fun attachmentsEndpoint(uploadable: Uploadable): URL {
        val measurementId = uploadable.measurementId()
        val deviceId = uploadable.deviceId()
        return URL(returnUrlWithTrailingSlash(apiEndpoint) + "measurements/$deviceId/$measurementId/attachments")
    }

    override fun onUploadFinished(uploadable: Uploadable) {
        // Nothing to do (only required by WebdavUploader)
    }

    @Throws(UploadFailed::class)
    private fun uploadFile(
        jwtToken: String,
        uploadable: Uploadable,
        file: File,
        endpoint: URL,
        progressListener: UploadProgressListener
    ): Result {
        return try {
            FileInputStream(file).use { fis ->
                val uploader = initializeUploader(jwtToken, uploadable, fis, file)

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
                uploader.metadata = JsonHttpContent(jsonFactory, uploadable.toMap())

                // Vert.X currently only supports compressing "down-stream" out of the box
                uploader.disableGZipContent = true

                // Progress
                uploader.progressListener = ProgressHandler(progressListener)

                // Upload
                val requestUrl = GenericUrl(endpoint)
                val response = uploader.upload(requestUrl)
                try {
                    readResponse(response, jsonFactory)
                } finally {
                    response.disconnect()
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            handleUploadException(e)
        }
    }

    private fun initializeUploader(
        jwtToken: String,
        uploadable: Uploadable,
        fileInputStream: FileInputStream,
        file: File
    ): MediaHttpUploader {
        val bufferedInputStream = BufferedInputStream(fileInputStream)
        val mediaContent = InputStreamContent("application/octet-stream", bufferedInputStream)
        mediaContent.length = file.length()
        LOGGER.debug("mediaContent.length: ${mediaContent.length}")
        val transport = NetHttpTransport() // Use Builder to modify behaviour
        val jwtBearer = "Bearer $jwtToken"
        val httpRequestInitializer = RequestInitializeHandler(uploadable, jwtBearer)
        return MediaHttpUploader(mediaContent, transport, httpRequestInitializer)
    }

    /**
     * Handles exceptions thrown during upload.
     *
     * We wrap errors with [UploadFailed] so that the caller can handle this without crashing.
     * This way the SDK's `SyncPerformer` can determine if the sync should be repeated.
     */
    private fun handleUploadException(exception: Exception): Nothing {
        when (exception) {
            // Crash unexpected errors hard
            is MalformedURLException -> error(exception)

            // Soft caught errors

            // File is too large to be uploaded. Handle in caller (e.g. skip the upload).
            // The max size is currently static and set to 100 MB which should be about 44 hours of 100 Hz measurement.
            is MeasurementTooLarge,
            // `HTTP_BAD_REQUEST` (400).
            is BadRequestException,
            // `HTTP_UNAUTHORIZED` (401).
            is UnauthorizedException,
            // `HTTP_FORBIDDEN` (403). Seems to happen when server is unavailable. Handle in caller.
            is ForbiddenException,
            // `HTTP_CONFLICT` (409). Already uploaded. Handle in caller (e.g. mark as synced).
            is ConflictException,
            // `HTTP_ENTITY_NOT_PROCESSABLE` (422).
            is EntityNotParsableException,
            // `HTTP_INTERNAL_ERROR` (500).
            is InternalServerErrorException,
            // `HTTP_TOO_MANY_REQUESTS` (429). Try again later.
            is TooManyRequestsException,
            // IOException while reading the response. Try again later.
            is SynchronisationException,
            // `HTTP_NOT_FOUND` (404). Try again.
            is UploadSessionExpired,
            // Unexpected response code. Should be reported to the server admin.
            is UnexpectedResponseCode,
            // `PRECONDITION_REQUIRED` (428). Shouldn't happen during upload, report to server admin.
            is AccountNotActivated

            -> throw UploadFailed(exception)

            // This is not yet thrown as a specific exception.
            // Network without internet connection. Try again later.
            // is HostUnresolvable -> throw LoginFailed(e)

            // Happened on emulator when endpoint is local network instead of 10.0.2.2 [DAT-727]
            // Server not reachable. Try again later.
            is SocketTimeoutException -> throw UploadFailed(ServerUnavailableException(exception))

            is SSLException, is InterruptedIOException, is IOException -> handleNetworkException(exception)

            else -> throw UploadFailed(SynchronisationException(exception))
        }
    }

    private fun handleNetworkException(exception: Exception): Nothing {
        fun logAndThrow(message: String, e: Exception, wrap: (Exception) -> Exception): Nothing {
            LOGGER.warn("$message: ${e.message}")
            throw wrap(e)
        }

        when (exception) {
            is SSLException -> {
                // Thrown by OkHttp when the network is no longer available [DAT-740]. Try again later.
                if (exception.message?.contains("I/O error during system call, Broken pipe") == true) {
                    logAndThrow("Network became unavailable during upload", exception) {
                        UploadFailed(NetworkUnavailableException(it))
                    }
                }
                logAndThrow("Caught SSLException", exception) {
                    UploadFailed(SynchronisationException(it))
                }
            }
            is InterruptedIOException -> {
                // Request interrupted [DAT-741]. Try again later.
                if (exception.message?.contains("thread interrupted") == true) {
                    logAndThrow("Network interrupted during upload", exception) {
                        UploadFailed(NetworkUnavailableException(it))
                    }
                }
                // InterruptedIOException while reading the response. Try again later.
                logAndThrow("Caught InterruptedIOException", exception) {
                    UploadFailed(SynchronisationException(it))
                }
            }
            is IOException -> {
                // Unstable Wi-Fi connection [DAT-742]. transmission stream ended too early, likely because the sync
                // thread was interrupted (sync canceled). Try again later.
                if (exception.message?.contains("unexpected end of stream") == true) {
                    logAndThrow("Upload interrupted", exception, ::SynchronizationInterruptedException)
                }
                // IOException while reading the response. Try again later.
                logAndThrow("Caught IOException", exception, ::SynchronisationException)
            }
            else -> throw exception
        }
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

    internal class ProgressHandler(private val progressListener: UploadProgressListener) :
        MediaHttpUploaderProgressListener {

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
         * Adds a trailing slash to the server URL or leaves an existing trailing slash untouched.
         *
         * @param url The url to format.
         * @return The server URL with a trailing slash.
         */
        fun returnUrlWithTrailingSlash(url: String): String {
            return if (url.endsWith("/")) {
                url
            } else {
                "$url/"
            }
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
        @Suppress("MemberVisibilityCanBePrivate") // Part of the API
        @JvmStatic
        fun readInputStream(inputStream: InputStream): String {
            return try {
                BufferedReader(InputStreamReader(inputStream, DEFAULT_CHARSET)).use { bufferedReader ->
                    val responseString = StringBuilder()
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        responseString.append(line)
                    }
                    responseString.toString()
                }
            } catch (e: UnsupportedEncodingException) {
                error(e)
            } catch (e: IOException) {
                error(e)
            }
        }
    }
}
