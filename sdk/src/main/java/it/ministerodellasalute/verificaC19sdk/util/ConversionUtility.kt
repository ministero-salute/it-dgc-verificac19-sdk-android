/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
 *  ---
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ---license-end
 *
 *  Created by nicolamcornelio on 28/09/2021, 14:06
 */

package it.ministerodellasalute.verificaC19sdk.util

object ConversionUtility {

    fun byteToMegaByte(byteValue: Float): Float {
        return try {
            byteValue / 1048576
        } catch (e: Exception) {
            0f
        }
    }

    fun megaByteToByte(megaByteValue: Float): Float {
        return megaByteValue * 1048576
    }

    fun stringToBoolean(stringValue: String): Boolean {
        return stringValue == "true"
    }
}

