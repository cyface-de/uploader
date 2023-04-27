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
 * An `Exception` thrown when the host of the Cyface server cannot be resolved.
 *
 *
 * This is e.g. the case when the phone is connected to a Wi-Fi which is not connected to the internet.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 6.1.0-beta1
 */
class HostUnresolvable : Exception {
    /**
     * @param detailedMessage A more detailed message explaining the context for this `Exception`.
     */
    constructor(detailedMessage: String?) : super(detailedMessage)

    /**
     * @param detailedMessage A more detailed message explaining the context for this `Exception`.
     * @param cause The `Exception` that caused this one.
     */
    constructor(detailedMessage: String?, cause: Exception?) : super(detailedMessage, cause)

    /**
     * @param cause The `Exception` that caused this one.
     */
    constructor(cause: Exception?) : super(cause)
}
