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

import de.cyface.model.RequestMetaData
import de.cyface.uploader.exception.AccountNotActivated
import de.cyface.uploader.exception.BadRequestException
import de.cyface.uploader.exception.ConflictException
import de.cyface.uploader.exception.EntityNotParsableException
import de.cyface.uploader.exception.ForbiddenException
import de.cyface.uploader.exception.HostUnresolvable
import de.cyface.uploader.exception.InternalServerErrorException
import de.cyface.uploader.exception.MeasurementTooLarge
import de.cyface.uploader.exception.NetworkUnavailableException
import de.cyface.uploader.exception.ServerUnavailableException
import de.cyface.uploader.exception.SynchronisationException
import de.cyface.uploader.exception.SynchronizationInterruptedException
import de.cyface.uploader.exception.TooManyRequestsException
import de.cyface.uploader.exception.UnauthorizedException
import de.cyface.uploader.exception.UnexpectedResponseCode
import de.cyface.uploader.exception.UploadSessionExpired
import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Interface for uploading files to a Cyface Data Collector.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
interface Uploader {

    /**
     * Uploads the provided file to the server available at the [endpoint].
     *
     * @param jwtToken A String in the format "eyXyz123***".
     * @param metaData The [RequestMetaData] required for the Multipart request.
     * @param file The data file to upload via this post request.
     * @param progressListener The [UploadProgressListener] to be informed about the upload progress.
     * @throws SynchronisationException If an IOException occurred during synchronization.
     * @throws BadRequestException When server returns `HttpURLConnection#HTTP_BAD_REQUEST`
     * @throws UnauthorizedException When the server returns `HttpURLConnection#HTTP_UNAUTHORIZED`
     * @throws ForbiddenException When the server returns `HttpURLConnection#HTTP_FORBIDDEN`
     * @throws ConflictException When the server returns `HttpURLConnection#HTTP_CONFLICT`
     * @throws EntityNotParsableException When the server returns [DefaultUploader.HTTP_ENTITY_NOT_PROCESSABLE]
     * @throws InternalServerErrorException When the server returns `HttpURLConnection#HTTP_INTERNAL_ERROR`
     * @throws NetworkUnavailableException When the network used for transmission becomes unavailable.
     * @throws SynchronizationInterruptedException When the transmission stream ended too early, likely because the sync
     * thread was interrupted (sync canceled)
     * @throws TooManyRequestsException When the server returns [DefaultUploader.HTTP_TOO_MANY_REQUESTS]
     * @throws HostUnresolvable e.g. when the phone is connected to a network which is not connected to the internet
     * @throws ServerUnavailableException When no connection could be established with the server
     * @throws MeasurementTooLarge When the transfer file is too large to be uploaded.
     * @throws UnexpectedResponseCode When the server returns an unexpected response code
     * @throws AccountNotActivated When the user account is not activated
     * @return [Result.UPLOAD_SUCCESSFUL] on success, [Result.UPLOAD_FAILED] when the upload attempt should be repeated
     * or [Result.UPLOAD_SKIPPED] if the server is not interested in the data.
     */
    @Throws(
        BadRequestException::class,
        UnauthorizedException::class,
        ForbiddenException::class,
        ConflictException::class,
        EntityNotParsableException::class,
        InternalServerErrorException::class,
        TooManyRequestsException::class,
        ServerUnavailableException::class,
        SynchronisationException::class,
        SynchronizationInterruptedException::class,
        UploadSessionExpired::class,
        UnexpectedResponseCode::class,
        AccountNotActivated::class,
        NetworkUnavailableException::class,
        MeasurementTooLarge::class
    )
    @Suppress("unused") // Part of the API
    fun upload(
        jwtToken: String,
        metaData: RequestMetaData,
        file: File,
        progressListener: UploadProgressListener
    ): Result

    /**
     * @return the endpoint which will be used for the upload.
     */
    @Throws(
        MalformedURLException::class
    )
    fun endpoint(): URL
}