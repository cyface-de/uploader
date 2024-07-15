/*
 * Copyright 2018-2024 Cyface GmbH
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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import de.cyface.uploader.model.Measurement
import de.cyface.uploader.model.MeasurementIdentifier
import de.cyface.uploader.model.metadata.ApplicationMetaData
import de.cyface.uploader.model.metadata.AttachmentMetaData
import de.cyface.uploader.model.metadata.DeviceMetaData
import de.cyface.uploader.model.metadata.GeoLocation
import de.cyface.uploader.model.metadata.MeasurementMetaData
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Tests whether our default implementation of the HTTP protocol works as expected.
 *
 * @author Armin Schnabel
 */
class DefaultUploaderTest {
    /**
     * Tests that the body of the pre-request stays consistent.
     */
    @Test
    fun testPreRequestBody() {
        val deviceId = UUID.randomUUID()
        val measurementId = 78L
        val startLocation = GeoLocation(1000000000L, 51.1, 13.1)
        val endLocation = GeoLocation(1000010000L, 51.2, 13.2)
        val uploadable = Measurement(
            MeasurementIdentifier(deviceId, measurementId),
            DeviceMetaData(
                "test_osVersion",
                "test_deviceType",
            ),
            ApplicationMetaData(
                "test_appVersion",
                3,
            ),
            MeasurementMetaData(
                10.0,
                5,
                startLocation,
                endLocation,
                "BICYCLE",
            ),
            AttachmentMetaData(
                0,
                0,
                0,
                0,
            ),
        )

        // Act
        val result: Map<String, String> = uploadable.toMap()

        // Assert
        val expected: MutableMap<String, String> = HashMap()
        expected["startLocLat"] = "51.1"
        expected["startLocLon"] = "13.1"
        expected["startLocTS"] = "1000000000"
        expected["endLocLat"] = "51.2"
        expected["endLocLon"] = "13.2"
        expected["endLocTS"] = "1000010000"
        expected["deviceId"] = deviceId.toString()
        expected["measurementId"] = "78"
        expected["deviceType"] = "test_deviceType"
        expected["osVersion"] = "test_osVersion"
        expected["appVersion"] = "test_appVersion"
        expected["length"] = "10.0"
        expected["locationCount"] = "5"
        expected["modality"] = "BICYCLE"
        expected["formatVersion"] = "3"
        expected["logCount"] = "0"
        expected["imageCount"] = "0"
        expected["videoCount"] = "0"
        expected["filesSize"] = "0"
        assertThat(result, equalTo(expected))
    }
}
