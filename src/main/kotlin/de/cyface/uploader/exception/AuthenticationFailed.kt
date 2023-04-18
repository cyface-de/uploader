/*
 * Copyright 2020-2023 Cyface GmbH
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
 * An `Exception` that is thrown if authentication with a Cyface data collector service failed for some
 * reason.
 * Further details about the reason of the failure are either available from the wrapped exception, or from the error
 * message.
 *
 * @author Klemens Muthmann
 */
class AuthenticationFailed : Exception {
    /**
     * Creates a new completely initialized object of this class.
     *
     * @param message The error message describing the failure
     */
    constructor(message: String) : super(message)

    /**
     * Creates a new completely initialized object of this class.
     *
     * @param cause Another `Exception` being the cause for this one
     */
    constructor(cause: Exception) : super(cause)
}