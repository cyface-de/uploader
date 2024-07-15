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
package de.cyface.uploader.model.metadata

import de.cyface.uploader.exception.DeprecatedFormatVersion
import de.cyface.uploader.exception.UnknownFormatVersion
import de.cyface.uploader.model.metadata.MetaData.Companion.MAX_GENERIC_METADATA_FIELD_LENGTH
import java.io.Serializable

/**
 * The metadata which describes the application which collected the data.
 *
 * @author Armin Schnabel
 * @property applicationVersion The version of the app that transmitted the measurement.
 * @property formatVersion The format version of the upload file.
 */
data class ApplicationMetaData(
    val applicationVersion: String,
    val formatVersion: Int,
) : MetaData, Serializable {
    init {
        require(applicationVersion.isNotEmpty() && applicationVersion.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field applicationVersion had an invalid length of ${applicationVersion.length.toLong()}"
        }
        if (formatVersion < CURRENT_TRANSFER_FILE_FORMAT_VERSION) {
            throw DeprecatedFormatVersion("Deprecated formatVersion: ${formatVersion.toLong()}")
        } else if (formatVersion != CURRENT_TRANSFER_FILE_FORMAT_VERSION) {
            throw UnknownFormatVersion("Unknown formatVersion: ${formatVersion.toLong()}")
        }
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 1L

        /**
         * The current version of the transferred file. This is always specified by the first two bytes of the file
         * transferred and helps compatible APIs to process data from different client versions.
         */
        const val CURRENT_TRANSFER_FILE_FORMAT_VERSION = 3
    }
}
