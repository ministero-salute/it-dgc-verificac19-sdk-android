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
 *  Created by danieliulianrotaru on 5/25/21 3:44 PM
 */

package it.ministerodellasalute.verificaC19sdk.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.security.MessageDigest
import java.util.*

/**
 *
 * This object contains useful general utilities.
 *
 */
object Utility {
    /**
     *
     * This method compares two versions of the app, [v1] and [v2], returning the result of this
     * comparison as an [Int] value.
     *
     */
    fun versionCompare(v1: String, v2: String): Int {
        // vnum stores each numeric part of version
        var vnum1 = 0
        var vnum2 = 0

        // loop until both String are processed
        var i = 0
        var j = 0
        while (i < v1.length || j < v2.length) {

            // Store numeric part of version 1 in vnum1
            while (i < v1.length && v1[i] != '.') {
                vnum1 = (vnum1 * 10 + (v1[i] - '0'))
                i++
            }

            // store numeric part of version 2 in vnum2
            while (j < v2.length && v2[j] != '.') {
                vnum2 = (vnum2 * 10 + (v2[j] - '0'))
                j++
            }
            if (vnum1 > vnum2) return 1
            if (vnum2 > vnum1) return -1

            // if equal, reset variables and go for next numeric part
            vnum2 = 0
            vnum1 = vnum2
            i++
            j++
        }
        return 0
    }

    private fun encodeBase64(input: ByteArray?): String {
        return if (Build.VERSION.SDK_INT >= 26) {
            Base64.getEncoder().encodeToString(input)
        } else {
            android.util.Base64.encodeToString(input, android.util.Base64.NO_WRAP)
        }
    }

    fun String.sha256(): String {
        return hashString(this, "SHA-256")
    }

    private fun hashString(input: String, algorithm: String): String {
        return encodeBase64(
            MessageDigest
            .getInstance(algorithm)
            .digest(input.toByteArray()))
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return true
        }
        return false
    }
}