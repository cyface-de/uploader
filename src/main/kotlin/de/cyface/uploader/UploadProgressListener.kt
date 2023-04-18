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

/**
 * Listens for progress during a data upload and reports the progress in percent.
 *
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 2.0.0
 */
interface UploadProgressListener {
    /**
     * Reports the progress of the current data upload.
     *
     * @param percent The data upload progress in percent (between 0.0 and 100.0).
     */
    fun updatedProgress(percent: Float)
}