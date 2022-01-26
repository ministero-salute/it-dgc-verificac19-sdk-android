/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2022 T-Systems International GmbH and all other contributors
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
 *  Created by nicolamcornelio on 1/26/22, 2:19 PM
 */

package it.ministerodellasalute.verificaC19sdk.utility

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream

class AndroidTestUtility {

    companion object {

        fun openAndroidTestAssetsFile(fileName: String): InputStream {
            val testContext: Context = ApplicationProvider.getApplicationContext()
            return testContext.assets.open(fileName)
        }

        @Throws(IOException::class)
        fun readInputStream(inputStream: InputStream): String {
            val bufferedReader = BufferedReader(inputStream.reader())
            bufferedReader.use {
                return it.readText()
            }
        }
    }
}