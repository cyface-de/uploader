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

import de.cyface.uploader.exception.InvalidMetaData
import de.cyface.uploader.model.metadata.ApplicationMetaData
import de.cyface.uploader.model.metadata.AttachmentMetaData
import de.cyface.uploader.model.metadata.DeviceMetaData
import de.cyface.uploader.model.metadata.MeasurementMetaData
import java.util.UUID

data class Measurement(
    val identifier: MeasurementIdentifier,
    private val deviceMetaData: DeviceMetaData,
    private val applicationMetaData: ApplicationMetaData,
    private val measurementMetaData: MeasurementMetaData,
    private val attachmentMetaData: AttachmentMetaData,
) : Uploadable {
    override fun toMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        map[FormAttributes.DEVICE_ID.value] = identifier.deviceIdentifier.toString()
        map[FormAttributes.MEASUREMENT_ID.value] = identifier.measurementIdentifier.toString()

        map[FormAttributes.OS_VERSION.value] = deviceMetaData.operatingSystemVersion
        map[FormAttributes.DEVICE_TYPE.value] = deviceMetaData.deviceType

        map[FormAttributes.APPLICATION_VERSION.value] = applicationMetaData.applicationVersion
        map[FormAttributes.FORMAT_VERSION.value] = applicationMetaData.formatVersion.toString()

        measurementMetaData.startLocation?.let { startLocation ->
            map[FormAttributes.START_LOCATION_LAT.value] = startLocation.latitude.toString()
            map[FormAttributes.START_LOCATION_LON.value] = startLocation.longitude.toString()
            map[FormAttributes.START_LOCATION_TS.value] = startLocation.timestamp.toString()
        }
        measurementMetaData.endLocation?.let { endLocation ->
            map[FormAttributes.END_LOCATION_LAT.value] = endLocation.latitude.toString()
            map[FormAttributes.END_LOCATION_LON.value] = endLocation.longitude.toString()
            map[FormAttributes.END_LOCATION_TS.value] = endLocation.timestamp.toString()
        }
        map[FormAttributes.LENGTH.value] = measurementMetaData.length.toString()
        map[FormAttributes.LOCATION_COUNT.value] = measurementMetaData.locationCount.toString()
        map[FormAttributes.MODALITY.value] = measurementMetaData.modality

        map[FormAttributes.LOG_COUNT.value] = attachmentMetaData.logCount.toString()
        map[FormAttributes.IMAGE_COUNT.value] = attachmentMetaData.imageCount.toString()
        map[FormAttributes.VIDEO_COUNT.value] = attachmentMetaData.videoCount.toString()
        map[FormAttributes.FILES_SIZE.value] = attachmentMetaData.filesSize.toString()

        return map
    }
}

data class MeasurementIdentifier(val deviceIdentifier: UUID, val measurementIdentifier: Long)

class MeasurementFactory : UploadableFactory {
    override fun attachmentMetaData(
        logCount: String?,
        imageCount: String?,
        videoCount: String?,
        filesSize: String?
    ): AttachmentMetaData {
        // For backward compatibility we support measurement upload requests without attachment metadata
        val attachmentMetaMissing = logCount == null && imageCount == null && videoCount == null && filesSize == null
        if (attachmentMetaMissing) {
            return AttachmentMetaData(0, 0, 0, 0)
        } else {
            validateAttachmentMetaData(logCount, imageCount, videoCount, filesSize)
            return AttachmentMetaData(
                logCount!!.toInt(),
                imageCount!!.toInt(),
                videoCount!!.toInt(),
                filesSize!!.toLong(),
            )
        }
    }

    private fun validateAttachmentMetaData(
        logCount: String?,
        imageCount: String?,
        videoCount: String?,
        filesSize: String?
    ) {
        if (logCount == null) throw InvalidMetaData("Data incomplete logCount was null!")
        if (imageCount == null) throw InvalidMetaData("Data incomplete imageCount was null!")
        if (videoCount == null) throw InvalidMetaData("Data incomplete videoCount was null!")
        if (filesSize == null) throw InvalidMetaData("Data incomplete filesSize was null!")
        if (logCount.toInt() < 0 || imageCount.toInt() < 0 || videoCount.toInt() < 0) {
            throw InvalidMetaData("Invalid file count for attachment.")
        }
        val attachmentCount = logCount.toInt() + imageCount.toInt() + videoCount.toInt()
        if (attachmentCount > 0 && filesSize.toLong() <= 0L) {
            throw InvalidMetaData("Files size for attachment must be greater than 0.")
        }
    }
}
