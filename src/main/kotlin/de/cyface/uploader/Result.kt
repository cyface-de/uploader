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

/**
 * Result returned by this class' public methods, to inform callers how to proceed.
 *
 * @author Armin Schnabel
 * @version 1.0.1
 * @since 1.0.0
 */
enum class Result {
    UPLOAD_SUCCESSFUL,
    UPLOAD_SKIPPED,
    @Suppress("unused") // Used by `android-backend.SyncPerformer`
    UPLOAD_FAILED,
    LOGIN_SUCCESSFUL
}