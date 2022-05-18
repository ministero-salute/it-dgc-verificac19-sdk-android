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
 *  Created by osarapulov on 4/30/21 5:01 PM
 */

package it.ministerodellasalute.verificaC19sdk.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.methodName

/**
 *
 * This class represents the [CoroutineWorker] of the SDK.
 *
 */
@HiltWorker
class LoadKeysWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workParams: WorkerParameters,
    private val verifierRepository: VerifierRepository
) : CoroutineWorker(context, workParams) {

    /**
     *
     * This method represents the periodic asynchronously  work that the Work Manager accomplishes
     * each 1 day on the background.
     *
     */
    override suspend fun doWork(): Result {
        Log.v(methodName(), "key fetching start")
        val res = verifierRepository.syncData(applicationContext)
        Log.v(methodName(), "key fetching result: ${res == true}")
        return if (res == true) Result.success() else Result.retry()
    }
}