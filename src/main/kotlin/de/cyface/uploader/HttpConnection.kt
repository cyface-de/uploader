/*
 * Copyright 2017-2023 Cyface GmbH
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

import de.cyface.uploader.DefaultUploader.Companion.DEFAULT_CHARSET
import de.cyface.uploader.exception.SynchronisationException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

/**
 * Implements the [Http] connection interface for the Cyface apps.
 *
 * @author Klemens Muthmann
 * @author Armin Schnabel
 * @version 14.0.0
 * @since 2.0.0
 */
class HttpConnection : Http {

    override fun open(url: URL, hasBinaryContent: Boolean): HttpURLConnection {
        val connection = try {
            url.openConnection() as HttpURLConnection
        } catch (e: IOException) {
            // openConnection() only prepares, but does not establish an actual network connection
            throw SynchronisationException("Error ${e.message}. Unable to prepare connection for URL $url.", e)
        }
        if (url.path.startsWith("https://")) {
            val httpsURLConnection = connection as HttpsURLConnection
            // Without verifying the hostname we receive the "Trust Anchor..." Error
            httpsURLConnection.hostnameVerifier =
                HostnameVerifier { _: String?, session: SSLSession? ->
                    val hv = HttpsURLConnection.getDefaultHostnameVerifier()
                    hv.verify(url.host, session)
                }
        }
        connection.setRequestProperty("Content-Type", "application/json; charset=$DEFAULT_CHARSET")
        try {
            connection.requestMethod = "POST"
        } catch (e: ProtocolException) {
            error(e)
        }
        connection.setRequestProperty("User-Agent", System.getProperty("http.agent"))
        return connection
    }
}
