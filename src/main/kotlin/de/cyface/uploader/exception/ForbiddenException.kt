/*
 * Copyright 2019-2023 Cyface GmbH
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
package de.cyface.uploader.exception

/**
 * An `Exception` thrown when the user is authorized but has no permissions to posts files.
 *
 * @author Armin Schnabel
 * @version 1.0.3
 * @since 1.0.0
 */
class ForbiddenException : Exception {
    /**
     * @param detailedMessage A more detailed message explaining the context for this `Exception`.
     */
    constructor(detailedMessage: String) : super(detailedMessage)

    /**
     * @param detailedMessage A more detailed message explaining the context for this `Exception`.
     * @param cause The `Exception` that caused this one.
     */
    @Suppress("unused") // Part of the API
    constructor(detailedMessage: String, cause: Exception) : super(detailedMessage, cause)
}