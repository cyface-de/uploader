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
import de.cyface.uploader.exception.UploadFailed
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
     * @throws UploadFailed when an error occurred.
     * @return [Result.UPLOAD_SUCCESSFUL] when successful and [Result.UPLOAD_SKIPPED] when the server is
     * not interested in the data.
     */
    @Throws(UploadFailed::class)
    @Suppress("unused") // Part of the API
    fun upload(
        jwtToken: String,
        metaData: RequestMetaData,
        file: File,
        progressListener: UploadProgressListener
    ): Result

    /**
     * @return the endpoint which will be used for the upload.
     * @throws MalformedURLException if the endpoint address provided is malformed.
     */
    @Throws(MalformedURLException::class)
    fun endpoint(): URL
}
