/*
 * Copyright 2024 Cyface GmbH
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
package de.cyface.uploader.model.metadata

import de.cyface.uploader.exception.TooFewLocations
import de.cyface.uploader.model.metadata.MetaData.Companion.MAX_GENERIC_METADATA_FIELD_LENGTH
import java.io.Serializable

/**
 * The metadata which describes the measurement the data was collected for.
 *
 * @author Armin Schnabel
 * @property length The length of the measurement in meters.
 * @property locationCount The count of geolocations in the transmitted measurement.
 * @property startLocation The first [GeoLocation] captured by the transmitted measurement, if available.
 * @property endLocation The last [GeoLocation] captured by the transmitted measurement, if available.
 * @property modality The modality type used to capture the measurement.
 */
data class MeasurementMetaData(
    val length: Double,
    val locationCount: Long,
    val startLocation: GeoLocation?,
    val endLocation: GeoLocation?,
    val modality: String,
) : MetaData, Serializable {
    init {
        // A measurement _without_ locations (`startLocation=null`) _is_ legitimate, as we want the server
        // to be able to decide if such measurements should be uploaded or not. [LEIP-1187]
        if (locationCount < MINIMUM_LOCATION_COUNT) {
            throw TooFewLocations("LocationCount smaller than required: $locationCount")
        }
        require(length >= MINIMUM_TRACK_LENGTH) {
            "Field length had an invalid value smaller then 0.0: $length"
        }
        require(modality.isNotEmpty() && modality.length <= MAX_GENERIC_METADATA_FIELD_LENGTH) {
            "Field modality had an invalid length of ${modality.length.toLong()}"
        }
    }

    companion object {
        /**
         * Used to serialize objects of this class. Only change this value if this classes attribute set changes.
         */
        @Suppress("ConstPropertyName")
        private const val serialVersionUID = 1L

        /**
         * The minimum length of a track stored with a measurement.
         */
        private const val MINIMUM_TRACK_LENGTH = 0.0

        /**
         * The minimum valid amount of locations stored inside a measurement.
         *
         * This is used on the client-side, so we need to allow < 2 locations to be sent, the server decides skipping.
         */
        private const val MINIMUM_LOCATION_COUNT = 0L
    }
}

/**
 * This class represents a geolocation at the start or end of a track.
 *
 * @author Armin Schnabel
 * @property timestamp The Unix timestamp this location was captured on in milliseconds.
 * @property latitude Geographical latitude (decimal fraction) raging from -90° (south) to 90° (north).
 * @property longitude Geographical longitude (decimal fraction) ranging from -180° (west) to 180° (east).
 */
data class GeoLocation(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double
)

/**
 * Factory to create [GeoLocation] objects from given parameters.
 *
 * @author Armin Schnabel
 */
class GeoLocationFactory {
    /**
     * Creates a new [GeoLocation] object from the given parameters.
     *
     * @param timestamp The timestamp of the location.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @return The created geographical location object or `null` if any of the parameters is `null`.
     */
    fun from(timestamp: String?, latitude: String?, longitude: String?): GeoLocation? {
        return if (timestamp != null && latitude != null && longitude != null) {
            GeoLocation(
                timestamp.toLong(),
                latitude.toDouble(),
                longitude.toDouble(),
            )
        } else {
            null
        }
    }
}
