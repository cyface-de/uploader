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

import de.cyface.uploader.exception.UploadFailed
import de.cyface.uploader.model.Attachment
import de.cyface.uploader.model.Measurement
import de.cyface.uploader.model.Uploadable
import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Interface for uploading files to a Cyface Data Collector.
 *
 * @author Armin Schnabel
 */
interface Uploader {

    /**
     * Uploads the provided measurement file to the server.
     *
     * @param jwtToken A String in the format "eyXyz123***".
     * @param uploadable The [Uploadable] describing the data to upload.
     * @param file The data file to upload via this post request.
     * @param progressListener The [UploadProgressListener] to be informed about the upload progress.
     * @throws UploadFailed when an error occurred.
     * @return [Result.UPLOAD_SUCCESSFUL] when successful and [Result.UPLOAD_SKIPPED] when the server is
     * not interested in the data.
     */
    @Throws(UploadFailed::class)
    fun uploadMeasurement(
        jwtToken: String,
        uploadable: Measurement,
        file: File,
        progressListener: UploadProgressListener
    ): Result

    /**
     * Uploads the provided attachment file to the server, associated with a specific measurement.
     *
     * @param jwtToken A String in the format "eyXyz123***".
     * @param uploadable The [Uploadable] describing the data to upload.
     * @param file The attachment file to upload via this post request.
     * @param fileName How the transfer file should be named when uploading.
     * @param progressListener The [UploadProgressListener] to be informed about the upload progress.
     * @throws UploadFailed when an error occurred.
     * @return [Result.UPLOAD_SUCCESSFUL] when successful and [Result.UPLOAD_SKIPPED] when the server is
     * not interested in the data.
     */
    @Throws(UploadFailed::class)
    fun uploadAttachment(
        jwtToken: String,
        uploadable: Attachment,
        file: File,
        fileName: String,
        progressListener: UploadProgressListener,
    ): Result

    /**
     * Determines the URL endpoint for uploading files associated with a specific measurement.
     *
     * @param uploadable The file to upload.
     * @return The URL endpoint used for uploading measurement files.
     * @throws MalformedURLException if the endpoint address is malformed.
     */
    @Throws(MalformedURLException::class)
    fun measurementsEndpoint(uploadable: Uploadable): URL

    /**
     * Determines the URL endpoint for uploading attachment files associated with a specific measurement.
     *
     * @param uploadable The file to upload.
     * @return The URL endpoint used for uploading attachment files.
     * @throws MalformedURLException if the endpoint address is malformed.
     */
    @Throws(MalformedURLException::class)
    fun attachmentsEndpoint(uploadable: Uploadable): URL
}
