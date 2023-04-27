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
import java.net.URL

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
     * @throws SynchronisationException If an IOException occurred while reading the response code.
     * @throws BadRequestException When server returns `HttpURLConnection#HTTP_BAD_REQUEST`
     * @throws UnauthorizedException When the server returns `HttpURLConnection#HTTP_UNAUTHORIZED`
     * @throws ForbiddenException When the server returns `HttpURLConnection#HTTP_FORBIDDEN`
     * @throws ConflictException When the server returns `HttpURLConnection#HTTP_CONFLICT`
     * @throws EntityNotParsableException When the server returns [DefaultUploader.HTTP_ENTITY_NOT_PROCESSABLE]
     * @throws InternalServerErrorException When the server returns `HttpURLConnection#HTTP_INTERNAL_ERROR`
     * @throws NetworkUnavailableException When the network used for transmission becomes unavailable.
     * @throws TooManyRequestsException When the server returns [DefaultUploader.HTTP_TOO_MANY_REQUESTS]
     * @throws HostUnresolvable e.g. when the phone is connected to a network which is not connected to the internet
     * @throws ServerUnavailableException When no connection could be established with the server
     * @throws UnexpectedResponseCode When the server returns an unexpected response code
     * @throws AccountNotActivated When the user account is not activated
     * @return The auth token as String. This token is only valid for some time. Just call this method before each
     * upload.
     */
    fun authenticate(username: String, password: String): String

    /**
     * @return the endpoint which will be used for authentication.
     */
    fun endpoint(): URL
}
