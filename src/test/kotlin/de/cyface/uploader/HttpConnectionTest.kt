/*
 * Copyright 2023 Cyface GmbH
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
import org.junit.jupiter.api.Test

/**
 * @author Armin Schnabel
 * @version 1.0.0
 * @since 1.0.0
 */
class HttpConnectionTest {
    @Test
    fun testCredentials() {
        // Arrange
        val http = HttpConnection()

        // Act
        val credentials = http.credentials("test@cyface.de", "secret")

        // Assert
        assertThat(credentials, equalTo("{\"username\":\"test@cyface.de\",\"password\":\"secret\"}"))
    }
}
