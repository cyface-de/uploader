/*
 * Copyright 2024 Cyface GmbH
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
package de.cyface.uploader.model

import de.cyface.uploader.model.metadata.AttachmentMetaData

/**
 * Interface for object types which the Collector accepts as upload.
 *
 * @author Klemens Muthmann
 */
interface Uploadable {
    /**
     * Transform this object into a `Map` representation which can be injected into the upload request header.
     */
    fun toMap(): Map<String, String>

    fun deviceId(): String

    fun measurementId(): Long

    fun timestamp(): Long?
}

/**
 * Factory interface for creating uploadable objects.
 *
 * @author Klemens Muthmann
 */
interface UploadableFactory :
    DeviceMetaDataFactory,
    ApplicationMetaDataFactory,
    MeasurementMetaDataFactory,
    AttachmentMetaDataFactory

/**
 * Factory for creating device-specific metadata objects.
 *
 * @author Klemens Muthmann
 */
interface DeviceMetaDataFactory

/**
 * Factory for creating application-specific metadata objects.
 *
 * @author Klemens Muthmann
 */
interface ApplicationMetaDataFactory

/**
 * Factory for creating measurement-specific metadata objects.
 *
 * @author Klemens Muthmann

 */
interface MeasurementMetaDataFactory

/**
 * Factory for creating attachment-specific metadata objects.
 *
 * @author Klemens Muthmann
 */
interface AttachmentMetaDataFactory {
    /**
     * Creates an attachment metadata object from the given values.
     *
     * @param logCount The number of log files captured for this measurement.
     * @param imageCount The number of image files captured for this measurement.
     * @param videoCount The number of video files captured for this measurement.
     * @param filesSize The number of bytes of the attachment files.
     * @return The created attachment metadata object.
     */
    fun attachmentMetaData(
        logCount: String?,
        imageCount: String?,
        videoCount: String?,
        filesSize: String?,
    ): AttachmentMetaData
}
