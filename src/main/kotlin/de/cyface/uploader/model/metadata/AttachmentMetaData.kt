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

import java.io.Serializable

/**
 * The metadata which describes the attachments collected together with the measurement.
 *
 * @author Armin Schnabel
 * @property logCount Number of log files captured for this measurement, e.g. image capturing metrics.
 * @property imageCount Number of image files captured for this measurement.
 * @property videoCount Number of video files captured for this measurement.
 * @property filesSize The number of bytes of the files collected for this measurement (log, image and video data).
 */
data class AttachmentMetaData(
    val logCount: Int,
    val imageCount: Int,
    val videoCount: Int,
    val filesSize: Long,
) : MetaData, Serializable {
    init {
        require(logCount >= 0) { "Invalid logCount: $logCount" }
        require(imageCount >= 0) { "Invalid imageCount: $imageCount" }
        require(videoCount >= 0) { "Invalid videoCount: $videoCount" }
        require(filesSize >= 0) { "Invalid filesSize: $filesSize" }
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 1L
    }
}
