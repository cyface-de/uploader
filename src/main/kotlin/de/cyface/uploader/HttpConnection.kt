/*
 * Copyright 2017-2023 Cyface GmbH
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

import de.cyface.model.Activation
import de.cyface.uploader.DefaultUploader.Companion.DEFAULT_CHARSET
import de.cyface.uploader.exception.AccountNotActivated
import de.cyface.uploader.exception.BadRequestException
import de.cyface.uploader.exception.ConflictException
import de.cyface.uploader.exception.EntityNotParsableException
import de.cyface.uploader.exception.ForbiddenException
import de.cyface.uploader.exception.HostUnresolvable
import de.cyface.uploader.exception.InternalServerErrorException
import de.cyface.uploader.exception.NetworkUnavailableException
import de.cyface.uploader.exception.ServerUnavailableException
import de.cyface.uploader.exception.SynchronisationException
import de.cyface.uploader.exception.TooManyRequestsException
import de.cyface.uploader.exception.UnauthorizedException
import de.cyface.uploader.exception.UnexpectedResponseCode
import de.cyface.uploader.exception.UploadSessionExpired
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSession

/**
 * Implements the [Http] connection interface for the Cyface apps.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 13.0.0
 * @since 2.0.0
 */
class HttpConnection : Http {

    override fun open(url: URL, hasBinaryContent: Boolean): HttpURLConnection {
        val connection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            // openConnection() only prepares, but does not establish an actual network connection
            throw SynchronisationException("Error ${e.message}. Unable to prepare connection for URL $url.", e)
        }
        if (url.path.startsWith("https://")) {
            val httpsURLConnection = connection as HttpsURLConnection
            // Without verifying the hostname we receive the "Trust Anchor..." Error
            httpsURLConnection.hostnameVerifier =
                HostnameVerifier { _: String?, session: SSLSession? ->
                    val hv = HttpsURLConnection.getDefaultHostnameVerifier()
                    hv.verify(url.host, session)
                }
        }
        connection.setRequestProperty("Content-Type", "application/json; charset=$DEFAULT_CHARSET")
        try {
            connection.requestMethod = "POST"
        } catch (e: ProtocolException) {
            error(e)
        }
        connection.setRequestProperty("User-Agent", System.getProperty("http.agent"))
        return connection
    }

    override fun login(
        connection: HttpURLConnection,
        username: String,
        password: String,
        compress: Boolean
    ): Result {
        // For performance reasons (documentation) set either fixedLength (known length) or chunked streaming mode
        // we currently don't use fixedLengthStreamingMode as we only use this request for small login requests
        connection.setChunkedStreamingMode(0)
        val credentials = credentials(username, password)
        val outputStream = initOutputStream(connection)
        try {
            LOGGER.debug("Transmitting with compression $compress.")
            if (compress) {
                connection.setRequestProperty("Content-Encoding", "gzip")
                outputStream.write(gzip(credentials.toByteArray(DEFAULT_CHARSET)))
            } else {
                outputStream.write(credentials.toByteArray(DEFAULT_CHARSET))
            }
            outputStream.flush()
            outputStream.close()
        } catch (e: SSLException) {
            // This exception is thrown by OkHttp when the network is no longer available
            val message = e.message
            if (message != null && message.contains("I/O error during system call, Broken pipe")) {
                LOGGER.warn("Caught SSLException: ${e.message}")
                throw NetworkUnavailableException("Network became unavailable during transmission.", e)
            } else {
                error(e) // SSLException with unknown cause
            }
        } catch (e: InterruptedIOException) {
            // This exception is thrown when the login request is interrupted, e.g. see MOV-761
            throw NetworkUnavailableException("Network interrupted during login", e)
        } catch (e: IOException) {
            error(e)
        }
        return try {
            readResponse(connection)
        } catch (e: UploadSessionExpired) {
            error(e) // unexpected for login
        }
    }

    override fun register(
        connection: HttpURLConnection,
        email: String,
        password: String,
        captcha: String,
        activation: Activation,
        group: String
    ): Result {
        // For performance reasons (documentation) set either fixedLength (known length) or chunked streaming mode
        // we currently don't use fixedLengthStreamingMode as we only use this request for small login requests
        connection.setChunkedStreamingMode(0)
        val payload = registrationPayload(email, password, captcha, activation, group)
        val outputStream = initOutputStream(connection)
        try {
            outputStream.write(payload.toByteArray(DEFAULT_CHARSET))
            outputStream.flush()
            outputStream.close()
        } catch (e: SSLException) {
            // This exception is thrown by OkHttp when the network is no longer available
            val message = e.message
            if (message != null && message.contains("I/O error during system call, Broken pipe")) {
                LOGGER.warn("Caught SSLException: ${e.message}")
                throw NetworkUnavailableException(
                    "Network became unavailable during transmission.",
                    e
                )
            } else {
                error(e) // SSLException with unknown cause
            }
        } catch (e: InterruptedIOException) {
            // This exception is thrown when the request is interrupted, e.g. see MOV-761
            throw NetworkUnavailableException("Network interrupted during login", e)
        } catch (e: IOException) {
            error(e)
        }
        return try {
            readResponse(connection)
        } catch (e: UploadSessionExpired) {
            error(e)
        }
    }

    fun credentials(username: String, password: String): String {
        return "{\"username\":\"$username\",\"password\":\"$password\"}"
    }

    private fun registrationPayload(
        email: String,
        password: String,
        captcha: String,
        template: Activation,
        group: String
    ): String {
        return "{\"email\":\"$email\",\"password\":\"$password\",\"captcha\":\"$captcha\",\"template\":\"" +
            "${template.name}\",\"group\":\"$group\"}"
    }

    private fun gzip(input: ByteArray): ByteArray {
        return try {
            var gzipOutputStream: GZIPOutputStream? = null
            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
                try {
                    gzipOutputStream.write(input)
                    gzipOutputStream.flush()
                } finally {
                    gzipOutputStream.close()
                }
                gzipOutputStream = null
                byteArrayOutputStream.toByteArray()
            } finally {
                gzipOutputStream?.close()
            }
        } catch (@Suppress("SwallowedException") e: IOException) {
            error("Failed to gzip.")
        }
    }

    /**
     * Initializes a `BufferedOutputStream` for the provided connection.
     *
     * @param connection the `HttpURLConnection` to create the stream for.
     * @return the `BufferedOutputStream` created.
     * @throws ServerUnavailableException When no connection could be established with the server
     * @throws HostUnresolvable e.g. when the phone is connected to a network which is not connected to the internet.
     */
    @Throws(ServerUnavailableException::class, HostUnresolvable::class)
    private fun initOutputStream(connection: HttpURLConnection): BufferedOutputStream {
        connection.doOutput = true // To upload data to the server
        return try {
            // Wrapping this in a Buffered steam for performance reasons
            BufferedOutputStream(connection.outputStream)
        } catch (e: IOException) {
            val message = e.message
            if (message != null && message.contains("Unable to resolve host")) {
                throw HostUnresolvable(e)
            }
            throw ServerUnavailableException(e)
        }
    }

    /**
     * Reads the [HttpResponse] from the [HttpURLConnection] and identifies known errors.
     *
     * @param connection The connection that received the response.
     * @return The [HttpResponse].
     * @throws SynchronisationException If an IOException occurred while reading the response code.
     * @throws BadRequestException When server returns `HttpURLConnection#HTTP_BAD_REQUEST`
     * @throws UnauthorizedException When the server returns `HttpURLConnection#HTTP_UNAUTHORIZED`
     * @throws ForbiddenException When the server returns `HttpURLConnection#HTTP_FORBIDDEN`
     * @throws ConflictException When the server returns `HttpURLConnection#HTTP_CONFLICT`
     * @throws EntityNotParsableException When the server returns [.HTTP_ENTITY_NOT_PROCESSABLE]
     * @throws InternalServerErrorException When the server returns `HttpURLConnection#HTTP_INTERNAL_ERROR`
     * @throws TooManyRequestsException When the server returns [.HTTP_TOO_MANY_REQUESTS]
     * @throws UploadSessionExpired When the server returns [HttpURLConnection.HTTP_NOT_FOUND]
     * @throws UnexpectedResponseCode When the server returns an unexpected response code
     * @throws AccountNotActivated When the user account is not activated
     */
    @Throws(
        SynchronisationException::class,
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
    private fun readResponse(connection: HttpURLConnection): Result {
        val responseCode: Int
        val responseMessage: String
        return try {
            responseCode = connection.responseCode
            responseMessage = connection.responseMessage
            val responseBody = readResponseBody(connection)
            if (responseCode in SUCCESS_CODE_START..SUCCESS_CODE_END) {
                DefaultUploader.handleSuccess(HttpResponse(responseCode, responseBody, responseMessage))
            } else {
                DefaultUploader.handleError(HttpResponse(responseCode, responseBody, responseMessage))
            }
        } catch (e: IOException) {
            throw SynchronisationException(e)
        }
    }

    /**
     * Reads the body from the [HttpURLConnection]. This contains either the error or the success message.
     *
     * @param connection the [HttpURLConnection] to read the response from
     * @return the [HttpResponse] body
     */
    private fun readResponseBody(connection: HttpURLConnection): String {
        // First try to read and return a success response body
        return try {
            DefaultUploader.readInputStream(connection.inputStream)
        } catch (@Suppress("SwallowedException") e: IOException) {
            // When reading the InputStream fails, we check if there is an ErrorStream to read from
            // (For details see https://developer.android.com/reference/java/net/HttpURLConnection)
            val errorStream = connection.errorStream ?: return ""

            // Return empty string if there were no errors, connection is not connected or server sent no useful data.
            // This occurred e.g. on Xiaomi Mi A1 after disabling Wi-Fi instantly after sync start
            DefaultUploader.readInputStream(errorStream)
        }
    }

    companion object {
        /**
         * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
         */
        private val LOGGER = LoggerFactory.getLogger(HttpConnection::class.java)

        private const val SUCCESS_CODE_START = 200
        private const val SUCCESS_CODE_END = 299
    }
}
