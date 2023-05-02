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

import de.cyface.model.Activation
import de.cyface.uploader.exception.AccountNotActivated
import de.cyface.uploader.exception.BadRequestException
import de.cyface.uploader.exception.ConflictException
import de.cyface.uploader.exception.EntityNotParsableException
import de.cyface.uploader.exception.ForbiddenException
import de.cyface.uploader.exception.HostUnresolvable
import de.cyface.uploader.exception.InternalServerErrorException
import de.cyface.uploader.exception.LoginFailed
import de.cyface.uploader.exception.NetworkUnavailableException
import de.cyface.uploader.exception.RegistrationFailed
import de.cyface.uploader.exception.ServerUnavailableException
import de.cyface.uploader.exception.SynchronisationException
import de.cyface.uploader.exception.TooManyRequestsException
import de.cyface.uploader.exception.UnauthorizedException
import de.cyface.uploader.exception.UnexpectedResponseCode
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.MalformedURLException
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

    @Suppress("CyclomaticComplexMethod")
    override fun authenticate(username: String, password: String): String {
        var connection: HttpURLConnection? = null
        val authToken: String
        try {
            connection = http.open(loginEndpoint(), false)

            // Try to send the request and handle expected errors
            val loginResponse = http.login(connection, username, password, false)
            LOGGER.debug("Response $loginResponse")

            // Make sure the successful response contains an Authorization token
            authToken = connection.getHeaderField("Authorization")
            check(!(loginResponse == Result.LOGIN_SUCCESSFUL && authToken == null)) {
                "Login successful but response does not contain a token"
            }

            return authToken
        }

        // Soft catch "expected" errors in `LoginFailed` exception so that the UI can handle this.
        // As this API is not used by other parties, there is no external definition of expected errors.
        catch (e: SynchronisationException) {
            throw LoginFailed(e) // IOException while reading the response. Try again later.
        } catch (e: UnauthorizedException) {
            throw LoginFailed(e) // `HTTP_UNAUTHORIZED` (401). Handle in UI.
        } catch (e: ForbiddenException) {
            throw LoginFailed(e) // `HTTP_FORBIDDEN` (403). Seems to happen when server is unavailable. Handle in UI.
        } catch (e: NetworkUnavailableException) {
            throw LoginFailed(e) // Network disappeared. Try again later.
        } catch (e: TooManyRequestsException) {
            throw LoginFailed(e) // `HTTP_TOO_MANY_REQUESTS` (429). Try again later.
        } catch (e: HostUnresolvable) {
            throw LoginFailed(e) // Network without internet connection. Try again later.
        } catch (e: ServerUnavailableException) {
            throw LoginFailed(e) // Server not reachable. Try again later.
        } catch (e: AccountNotActivated) {
            throw LoginFailed(e) // User account not activated. Handle in UI.
        } catch (e: UnexpectedResponseCode) {
            // We currently show a UI error. Is this also reported to Sentry? Then it's ok not to throw this hard.
            throw LoginFailed(e) // server returns an unexpected response code
        }

        // Crash unexpected errors hard
        catch (e: BadRequestException) {
            error(e) // `HTTP_BAD_REQUEST` (400).
        } catch (e: ConflictException) {
            error(e) // `HTTP_CONFLICT` (409).
        } catch (e: EntityNotParsableException) {
            error(e) // `HTTP_ENTITY_NOT_PROCESSABLE` (422).
        } catch (e: InternalServerErrorException) {
            // If this actually happens, we might want to catch it softly, report to Sentry and show a UI error.
            error(e) // `HTTP_INTERNAL_ERROR` (500).
        } catch (e: MalformedURLException) {
            error(e) // The endpoint url is malformed.
        } finally {
            connection?.disconnect()
        }
    }

    @Suppress("CyclomaticComplexMethod")
    override fun register(email: String, password: String, captcha: String, activation: Activation): Result {
        var connection: HttpURLConnection? = null
        try {
            connection = http.open(registrationEndpoint(), false)

            // Try to send the request and handle expected errors
            val response = http.register(connection, email, password, captcha, activation)
            LOGGER.debug("Response $response")
            return response
        }

        // Soft catch "expected" errors in `RegistrationFailed` exception so that the UI can handle this.
        // As this API is not used by other parties, there is no external definition of expected errors.
        catch (e: ConflictException) {
            throw RegistrationFailed(e) // `HTTP_CONFLICT` (409). Already registered. Handle in UI.
        } catch (e: SynchronisationException) {
            throw RegistrationFailed(e) // IOException while reading the response. Try again later.
        } catch (e: ForbiddenException) {
            // `HTTP_FORBIDDEN` (403). Seems to happen when server is unavailable. Handle in UI.
            throw RegistrationFailed(e)
        } catch (e: NetworkUnavailableException) {
            throw RegistrationFailed(e) // Network disappeared. Try again later.
        } catch (e: TooManyRequestsException) {
            throw RegistrationFailed(e) // `HTTP_TOO_MANY_REQUESTS` (429). Try again later.
        } catch (e: HostUnresolvable) {
            throw RegistrationFailed(e) // Network without internet connection. Try again later.
        } catch (e: ServerUnavailableException) {
            throw RegistrationFailed(e) // Server not reachable. Try again later.
        } catch (e: UnexpectedResponseCode) {
            // We currently show a UI error. Is this also reported to Sentry? Then it's ok not to throw this hard.
            throw RegistrationFailed(e) // server returns an unexpected response code
        }

        // Crash unexpected errors hard
        catch (e: BadRequestException) {
            error(e) // `HTTP_BAD_REQUEST` (400).
        } catch (e: UnauthorizedException) {
            error(e) // `HTTP_UNAUTHORIZED` (401).
        } catch (e: EntityNotParsableException) {
            error(e) // `HTTP_ENTITY_NOT_PROCESSABLE` (422).
        } catch (e: InternalServerErrorException) {
            error(e) // `HTTP_INTERNAL_ERROR` (500).
        } catch (e: AccountNotActivated) {
            error(e) // `PRECONDITION_REQUIRED` (428). Should not happen during registration.
        } catch (e: MalformedURLException) {
            error(e) // The endpoint url is malformed.
        } finally {
            connection?.disconnect()
        }
    }

    @Suppress("MemberVisibilityCanBePrivate") // Part of the API
    override fun loginEndpoint(): URL {
        return URL(returnUrlWithTrailingSlash(apiEndpoint) + "login")
    }

    @Suppress("MemberVisibilityCanBePrivate") // Part of the API
    override fun registrationEndpoint(): URL {
        return URL(returnUrlWithTrailingSlash(apiEndpoint) + "user")
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
