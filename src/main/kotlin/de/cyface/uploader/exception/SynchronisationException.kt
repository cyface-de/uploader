/*
 * Copyright 2021-2023 Cyface GmbH
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
 * An `Exception` thrown each time data synchronisation with the server fails. It provides further information either via
 * a message or another wrapped `Exception`.
 *
 * @author Klemens Muthmann
 * @version 1.1.4
 * @since 1.0.0
 */
class SynchronisationException : Exception {
    /**
     * Creates a new completely initialized `SynchronisationException`, wrapping another `Exception` from deeper within
     * the system.
     *
     * @param cause The wrapped `Exception`.
     */
    constructor(cause: Exception) : super(cause)

    /**
     * Creates a new completely initialized `SynchronisationException`, providing a detailed explanation
     * about the error.
     *
     * @param message The message explaining the error condition.
     */
    @Suppress("unused") // Part of the API
    constructor(message: String) : super(message)

    /**
     * Creates a new completely initialized `SynchronisationException`, providing a detailed explanation
     * about the error.
     *
     * @param message The message explaining the error condition.
     * @param cause The wrapped `Exception`.
     */
    @Suppress("unused") // Part of the API
    constructor(message: String, cause: Exception) : super(message, cause)
}