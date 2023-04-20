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
import java.net.HttpURLConnection
import java.net.URL

/**
 * An interface for http connections.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 12.0.0
 * @since 1.0.0
 */
interface Http {
    /**
     * A HTTPConnection must be opened with the right header before you can communicate with the Cyface REST API
     *
     * @param url The URL of the cyface backend's REST API.
     * @param hasBinaryContent True if binary content is to be transmitted
     * @return the HTTPURLConnection
     * @throws SynchronisationException When the connection object could not be prepared
     */
    @Throws(SynchronisationException::class)
    fun open(url: URL, hasBinaryContent: Boolean): HttpURLConnection

    /**
     * The post request which authenticates a user at the server.
     *
     * @param connection The `HttpURLConnection` to be used for the request.
     * @param username The username part of the credentials
     * @param password The password part of the credentials
     * @param compress True if the {@param payload} should get compressed
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
     * @return [Result.LOGIN_SUCCESSFUL] if successful or else an `Exception`.
     */
    @Throws(
        SynchronisationException::class,
        UnauthorizedException::class,
        BadRequestException::class,
        InternalServerErrorException::class,
        ForbiddenException::class,
        EntityNotParsableException::class,
        ConflictException::class,
        NetworkUnavailableException::class,
        TooManyRequestsException::class,
        HostUnresolvable::class,
        ServerUnavailableException::class,
        UnexpectedResponseCode::class,
        AccountNotActivated::class
    )
    fun login(connection: HttpURLConnection, username: String, password: String, compress: Boolean): Result


    /**
     * The post request which registers a new user at the server.
     *
     * @param connection The `HttpURLConnection` to be used for the request.
     * @param email The email part of the credentials
     * @param password The password part of the credentials
     * @param captcha The captcha token
     * @param activation The template to use for the activation email.
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
     * @return [Result.UPLOAD_SUCCESSFUL] if successful or else an `Exception`.
     */
    @Throws(
        SynchronisationException::class,
        UnauthorizedException::class,
        BadRequestException::class,
        InternalServerErrorException::class,
        ForbiddenException::class,
        EntityNotParsableException::class,
        ConflictException::class,
        NetworkUnavailableException::class,
        TooManyRequestsException::class,
        HostUnresolvable::class,
        ServerUnavailableException::class,
        UnexpectedResponseCode::class,
        AccountNotActivated::class
    )
    fun register(connection: HttpURLConnection, email: String, password: String, captcha: String, activation: Activation): Result
}