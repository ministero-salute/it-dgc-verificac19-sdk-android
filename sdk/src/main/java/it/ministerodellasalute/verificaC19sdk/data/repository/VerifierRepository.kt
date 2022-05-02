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
 *  Created by mykhailo.nester on 4/24/21 2:20 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import it.ministerodellasalute.verificaC19sdk.model.DebugInfoWrapper
import it.ministerodellasalute.verificaC19sdk.model.DrlFlowType
import it.ministerodellasalute.verificaC19sdk.model.drl.DownloadState
import java.security.cert.Certificate

/**
 *
 * This interface defines the methods to download public certificates (i.e. settings) and check
 * the download status. These are overridden by the implementing class [VerifierRepositoryImpl].
 *
 */
interface VerifierRepository {
    suspend fun syncData(applicationContext: Context): Boolean?
    suspend fun getCertificate(kid: String): Certificate?
    suspend fun downloadChunks(drlFlowType: DrlFlowType)
    suspend fun checkInBlackList(ucvi: String): Boolean
    fun getCertificateFetchStatus(): LiveData<Boolean>
    fun resetCurrentRetryStatus()
    fun getDebugInfoLiveData(): LiveData<DebugInfoWrapper>
    suspend fun callCRLStatus()
    fun setCertificateFetchStatus(fetchStatus: Boolean)
    fun setDebugInfoLiveData()
    fun getDownloadStatusLiveData(): LiveData<DownloadState>
    fun setDownloadStatus(downloadStatus: DownloadState)
}