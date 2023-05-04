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
package de.cyface.uploader

import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import de.cyface.model.RequestMetaData
import java.io.IOException

/**
 * Assembles a request as requested to upload data.
 *
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 7.0.0
 * @property metaData the `MetaData` used to request the upload permission from the server
 * @property jwtBearer the JWT token to authenticate the upload requests
 */
class RequestInitializeHandler(
    private val metaData: RequestMetaData,
    private val jwtBearer: String
) : HttpRequestInitializer {
    @Throws(IOException::class)

    override fun initialize(request: HttpRequest) {
        val headers = HttpHeaders()
        headers.authorization = jwtBearer
        addMetaData(metaData, headers)
        // sets the metadata in both requests but until we don't use the session-URI
        // feature we can't store the metadata from the pre-request to be used in the upload
        // and the library does not support just to set the upload request header
        request.headers = headers
    }

    private fun addMetaData(metaData: RequestMetaData, headers: HttpHeaders) {
        // Location meta data
        if (metaData.startLocation != null) {
            headers["startLocLat"] = metaData.startLocation.latitude.toString()
            headers["startLocLon"] = metaData.startLocation.longitude.toString()
            headers["startLocTS"] = metaData.startLocation.timestamp.toString()
        }
        if (metaData.endLocation != null) {
            headers["endLocLat"] = metaData.endLocation.latitude.toString()
            headers["endLocLon"] = metaData.endLocation.longitude.toString()
            headers["endLocTS"] = metaData.endLocation.timestamp.toString()
        }
        headers["locationCount"] = metaData.locationCount.toString()

        // Remaining meta data
        headers["deviceId"] = metaData.deviceIdentifier
        headers["measurementId"] = java.lang.Long.valueOf(metaData.measurementIdentifier).toString()
        headers["deviceType"] = metaData.deviceType
        headers["osVersion"] = metaData.operatingSystemVersion
        headers["appVersion"] = metaData.applicationVersion
        headers["length"] = metaData.length.toString()
        headers["modality"] = metaData.modality.toString()
        headers["formatVersion"] = metaData.formatVersion.toString()
    }
}
