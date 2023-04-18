/*
 * Copyright 2017-2023 Cyface GmbH
 *
 * This file is part of the Cyface SDK for Android.
 *
 * The Cyface SDK for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Cyface SDK for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Cyface SDK for Android. If not, see <http://www.gnu.org/licenses/>.
 */
package de.cyface.uploader

/**
 * Internal value object class for the attributes of an HTTP response. It wraps the HTTP
 * status code as well as the String body.
 *
 * @author Armin Schnabel
 * @author Klemens Muthmann
 * @version 4.0.1
 * @since 1.0.0
 * @property responseCode the HTTP status code returned by the server
 * @property body the HTTP response body returned by the server. Can be empty when the server has nothing to
 * @property responseMessage the HTTP status message returned by the server
 */
class HttpResponse(
    /**
     * The `HttpURLConnection` status code returned by the server's [HttpResponse].
     */
    val responseCode: Int,
    /**
     * The server's [HttpResponse] body.
     */
    val body: String,
    /**
     * The `HttpURLConnection` status message returned by the server's [HttpResponse].
     */
    val responseMessage: String
)