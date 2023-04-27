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

import de.cyface.uploader.exception.BadRequestException
import de.cyface.uploader.exception.ConflictException
import de.cyface.uploader.exception.EntityNotParsableException
import de.cyface.uploader.exception.InternalServerErrorException
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implementation of the [Authenticator].
 *
 * *Attention:* The authentication token is invalid after a few seconds.
 * Just call [DefaultAuthenticator.authenticate] again to get a new token.
 * Usually the token should be generated just before each [DefaultUploader.upload] call.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 * @property apiEndpoint An API endpoint running a Cyface data collector service, like `https://some.url/api/v3`
 */
@Suppress("unused") // Part of the API
class DefaultAuthenticator(private val apiEndpoint: String) : Authenticator {

    private val http: HttpConnection = HttpConnection()

    override fun authenticate(username: String, password: String): String {
        var connection: HttpURLConnection? = null
        val authToken: String
        try {
            connection = http.open(endpoint(), false)

            // Try to send the request and handle expected errors
            val loginResponse = http.login(connection, username, password, false)
            LOGGER.debug("Response $loginResponse")

            // Make sure the successful response contains an Authorization token
            authToken = connection.getHeaderField("Authorization")
            check(!(loginResponse == Result.LOGIN_SUCCESSFUL && authToken == null)) {
                "Login successful but response does not contain a token"
            }
        } catch (e: BadRequestException) {
            error(e) // API definition does not define those errors
        } catch (e: InternalServerErrorException) {
            error(e)
        } catch (e: EntityNotParsableException) {
            error(e)
        } catch (e: ConflictException) {
            error(e)
        } finally {
            connection?.disconnect()
        }

        return authToken
    }

    @Suppress("MemberVisibilityCanBePrivate") // Part of the API
    override fun endpoint(): URL {
        return URL(returnUrlWithTrailingSlash(apiEndpoint) + "login")
    }

    companion object {

        /**
         * The logger used to log messages from this class. Configure it using <tt>src/main/resources/logback.xml</tt>.
         */
        private val LOGGER = LoggerFactory.getLogger(DefaultAuthenticator::class.java)

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
    }
}
