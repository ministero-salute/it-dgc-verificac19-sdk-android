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
 *  Created by Mykhailo Nester on 4/23/21 9:48 AM
 */

package it.ministerodellasalute.verificaC19sdk

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import it.ministerodellasalute.verificaC19sdk.worker.LoadKeysWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 *
 * This class represents the [Application] of the SDK.
 *
 */

@HiltAndroidApp
class VerificaSDKApplication : Application() {

    init {
        instance = this
    }

    companion object {
        private var instance: VerificaSDKApplication? = null
        var isCertificateRevoked = false

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }


}
