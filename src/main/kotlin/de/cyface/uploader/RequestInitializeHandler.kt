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
import de.cyface.model.RequestMetaData
import java.io.IOException

/**
 * Assembles a request as requested to upload data.
 *
 * @author Armin Schnabel
 * @property metaData the `MetaData` used to request the upload permission from the server
 * @property jwtBearer the JWT token to authenticate the upload requests
 */
class RequestInitializeHandler<T : RequestMetaData.MeasurementIdentifier>(
    private val metaData: RequestMetaData<T>,
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

    private fun <T : RequestMetaData.MeasurementIdentifier> addMetaData(
        metaData: RequestMetaData<T>,
        headers: HttpHeaders
    ) {
        // Location meta data
        metaData.measurementMetaData.startLocation?.let { startLocation ->
            headers["startLocLat"] = startLocation.latitude.toString()
            headers["startLocLon"] = startLocation.longitude.toString()
            headers["startLocTS"] = startLocation.timestamp.toString()
        }
        metaData.measurementMetaData.endLocation?.let { endLocation ->
            headers["endLocLat"] = endLocation.latitude.toString()
            headers["endLocLon"] = endLocation.longitude.toString()
            headers["endLocTS"] = endLocation.timestamp.toString()
        }
        headers["locationCount"] = metaData.measurementMetaData.locationCount.toString()

        // Remaining meta data
        headers["deviceId"] = metaData.identifier.deviceId
        headers["measurementId"] = java.lang.Long.valueOf(metaData.identifier.measurementId).toString()
        headers["deviceType"] = metaData.deviceMetaData.deviceType
        headers["osVersion"] = metaData.deviceMetaData.operatingSystemVersion
        headers["appVersion"] = metaData.applicationMetaData.applicationVersion
        headers["length"] = metaData.measurementMetaData.length.toString()
        headers["modality"] = metaData.measurementMetaData.modality
        headers["formatVersion"] = metaData.applicationMetaData.formatVersion.toString()
        headers["logCount"] = metaData.attachmentMetaData.logCount
        headers["imageCount"] = metaData.attachmentMetaData.imageCount
        headers["videoCount"] = metaData.attachmentMetaData.videoCount
        headers["filesSize"] = metaData.attachmentMetaData.filesSize
    }
}
