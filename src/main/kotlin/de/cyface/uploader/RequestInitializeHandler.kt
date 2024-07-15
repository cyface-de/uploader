/*
 * Copyright 2021-2024 Cyface GmbH
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

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import de.cyface.uploader.model.Uploadable
import java.io.IOException

/**
 * Assembles a request as requested to upload data.
 *
 * @author Armin Schnabel
 * @property uploadable the `MetaData` used to request the upload permission from the server
 * @property jwtBearer the JWT token to authenticate the upload requests
 */
class RequestInitializeHandler(
    private val uploadable: Uploadable,
    private val jwtBearer: String
) : HttpRequestInitializer {
    @Throws(IOException::class)

    override fun initialize(request: HttpRequest) {
        val headers = HttpHeaders()
        headers.authorization = jwtBearer
        applyMetaToHttpHeaders(headers)
        // sets the metadata in both requests but until we don't use the session-URI
        // feature we can't store the metadata from the pre-request to be used in the upload
        // and the library does not support just to set the upload request header
        request.headers = headers
    }

    private fun applyMetaToHttpHeaders(headers: HttpHeaders) {
        val map = uploadable.toMap()
        map.forEach { (key, value) ->
            headers[key] = value
        }
    }
}
