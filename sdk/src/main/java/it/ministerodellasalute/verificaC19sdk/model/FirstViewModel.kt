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
 *  Created by danielsp on 9/15/21, 2:28 PM
 */

package it.ministerodellasalute.verificaC19sdk.model

import androidx.lifecycle.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.util.Utility
import javax.inject.Inject

@HiltViewModel
class FirstViewModel @Inject constructor(
    val verifierRepository: VerifierRepository,
    private val preferences: Preferences
) : ViewModel() {

    val fetchStatus: MediatorLiveData<Boolean> = MediatorLiveData()

    private val _scanMode = MutableLiveData<String>()
    val scanMode: LiveData<String> = _scanMode

    val maxRetryReached = MediatorLiveData<Boolean>().apply {
        value = false
    }

    val sizeOverLiveData = MediatorLiveData<Boolean>().apply {
        value = false
    }

    val initDownloadLiveData = MediatorLiveData<Boolean>().apply {
        value = false
    }

    val debugInfoLiveData = MediatorLiveData<DebugInfoWrapper>()


    fun getScanMode() = preferences.scanMode

    fun setScanMode(value: String) =
        run {
            preferences.scanMode = value
            _scanMode.value = value
        }

    fun getScanModeFlag() = preferences.hasScanModeBeenChosen

    fun setScanModeFlag(value: Boolean) =
        run { preferences.hasScanModeBeenChosen = value }

    init {
        preferences.shouldInitDownload = false
        fetchStatus.addSource(verifierRepository.getCertificateFetchStatus()) {
            fetchStatus.value = it
        }

        maxRetryReached.addSource(verifierRepository.getMaxRetryReached()) {
            maxRetryReached.value = it
        }
        sizeOverLiveData.addSource(verifierRepository.getSizeOverLiveData()){
            sizeOverLiveData.value = it
        }

        initDownloadLiveData.addSource(verifierRepository.getInitDownloadLiveData()){
            initDownloadLiveData.value = it
        }

        debugInfoLiveData.addSource(verifierRepository.getDebugInfoLiveData()) {
            debugInfoLiveData.value = it
        }
    }


    /**
     *
     * This method gets the date of last fetch from the Shared Preferences.
     *
     */
    fun getDateLastSync() = preferences.dateLastFetch

    fun getDrlDateLastSync() = preferences.drlDateLastFetch
    fun getTotalSizeInByte() = preferences.totalSizeInByte

    fun getSizeSingleChunkInByte() = preferences.sizeSingleChunkInByte
    fun getTotalChunk() = preferences.totalChunk //total number of chunks in a specific version
    fun getIsSizeOverThreshold() = preferences.isSizeOverThreshold
    fun getDownloadAvailable() = preferences.authorizedToDownload
    fun setDownloadAsAvailable() =
            run { preferences.authorizedToDownload = 1L }

    fun getResumeAvailable() = preferences.authToResume
    fun setResumeAsAvailable() =
            run { preferences.authToResume = 1L }

    fun setUnAuthResume() =
            run { preferences.authToResume = 0L }

    fun getIsPendingDownload(): Boolean {
        return preferences.currentVersion != preferences.requestedVersion
    }

    fun getIsDrlSyncActive() = preferences.isDrlSyncActive

    fun shouldInitDownload() = preferences.shouldInitDownload

    fun setShouldInitDownload(value: Boolean) = run {
        preferences.shouldInitDownload = value
    }
    fun getCurrentChunk() = preferences.currentChunk

    private fun getValidationRules(): Array<Rule> {
        val jsonString = preferences.validationRulesJson
        return Gson().fromJson(jsonString, Array<Rule>::class.java) ?: kotlin.run { emptyArray() }
    }

    fun getAppMinVersion(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value }
                ?.let {
                    it.value
                } ?: run {
            ""
        }
    }

    private fun getSDKMinVersion(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value }
                ?.let {
                    it.value
                } ?: run {
            ""
        }
    }

    fun isSDKVersionObsoleted(): Boolean {
        this.getSDKMinVersion().let {
            if (Utility.versionCompare(it, BuildConfig.SDK_VERSION) > 0) {
                return true
            }
        }
        return false
    }

    fun resetCurrentRetry() {
        verifierRepository.resetCurrentRetryStatus()
    }
}

