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

//import io.vertx.core.json.JsonObject

interface MetaData {
    //fun toJson(): JsonObject

    companion object {
        /**
         * Maximum size of a metadata field, with plenty space for future development. Prevents attackers from putting
         * arbitrary long data into these fields.
         */
        const val MAX_GENERIC_METADATA_FIELD_LENGTH = 30
    }
}
