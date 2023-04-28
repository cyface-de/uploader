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
import de.cyface.uploader.exception.LoginFailed
import de.cyface.uploader.exception.RegistrationFailed
import java.net.MalformedURLException
import java.net.URL
import kotlin.jvm.Throws

/**
 * Interface for authenticating to a Cyface Data Collector.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
interface Authenticator {

    /**
     * Authenticates with the Cyface data collector server available at the API endpoint.
     *
     * @param username The username of the user to authenticate
     * @param password The password of the user to authenticate
     * @throws LoginFailed when an expected error occurred, so that the UI can handle this.
     * @return The auth token as String. This token is only valid for some time. Just call this method before each
     * upload.
     */
    @Throws(LoginFailed::class)
    fun authenticate(username: String, password: String): String

    /**
     * Register a new user with the Cyface Data Collector server available at the API endpoint.
     *
     * @param email The email part of the credentials
     * @param password The password part of the credentials
     * @param captcha The captcha token
     * @param activation The template to use for the activation email.
     * @throws RegistrationFailed when an expected error occurred, so that the UI can handle this.
     * @return [Result.UPLOAD_SUCCESSFUL] if successful.
     */
    @Throws(RegistrationFailed::class)
    fun register(email: String, password: String, captcha: String, activation: Activation): Result

    /**
     * @return the endpoint which will be used for authentication.
     * @throws MalformedURLException if the endpoint address provided is malformed.
     */
    @Throws(MalformedURLException::class)
    fun loginEndpoint(): URL

    /**
     * @return the endpoint which will be used for registration.
     * @throws MalformedURLException if the endpoint address provided is malformed.
     */
    @Throws(MalformedURLException::class)
    fun registrationEndpoint(): URL
}
