/*
 * Copyright 2018-2023 Cyface GmbH
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

import de.cyface.model.MeasurementIdentifier
import de.cyface.model.Modality
import de.cyface.model.RequestMetaData
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests whether our default implementation of the HTTP protocol works as expected.
 *
 * @author Armin Schnabel
 * @version 2.1.0
 * @since 1.0.0
 */
class DefaultUploaderTest {
    /**
     * Tests that the body of the pre-request stays consistent.
     */
    @Test
    fun testPreRequestBody() {
        val deviceId = "testDevi-ce00-42b6-a840-1b70d30094b8" // Must be a valid UUID
        val id = MeasurementIdentifier(deviceId, 78)
        val startLocation = RequestMetaData.GeoLocation(1000000000L,51.1,13.1)
        val endLocation = RequestMetaData.GeoLocation(1000010000L,51.2,13.2)
        val metaData = RequestMetaData(
            id.deviceIdentifier,
            id.measurementIdentifier.toString(),
            "test_osVersion",
            "test_deviceType",
            "test_appVersion",
            10.0,
            5,
            startLocation,
            endLocation,
            Modality.BICYCLE.databaseIdentifier,
            3
        )

        // Act
        val result: Map<String, String> = DefaultUploader.preRequestBody(metaData)

        // Assert
        val expected: MutableMap<String, String> = HashMap()
        expected["startLocLat"] = "51.1"
        expected["startLocLon"] = "13.1"
        expected["startLocTS"] = "1000000000"
        expected["endLocLat"] = "51.2"
        expected["endLocLon"] = "13.2"
        expected["endLocTS"] = "1000010000"
        expected["deviceId"] = deviceId
        expected["measurementId"] = "78"
        expected["deviceType"] = "test_deviceType"
        expected["osVersion"] = "test_osVersion"
        expected["appVersion"] = "test_appVersion"
        expected["length"] = "10.0"
        expected["locationCount"] = "5"
        expected["modality"] = "BICYCLE"
        expected["formatVersion"] = "3"
        assertThat(result,`is`(equalTo(expected)))
    }
}