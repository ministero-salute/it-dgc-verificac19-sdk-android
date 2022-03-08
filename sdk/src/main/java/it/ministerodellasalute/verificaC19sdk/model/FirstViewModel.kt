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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.util.Utility
import javax.inject.Inject

@HiltViewModel
class FirstViewModel @Inject constructor(
    val verifierRepository: VerifierRepository,
    private val preferences: Preferences
) : ViewModel() {

    val fetchStatus: MediatorLiveData<Boolean> = MediatorLiveData()

    private val _scanMode = MutableLiveData<ScanMode>()
    val scanMode: LiveData<ScanMode> = _scanMode

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


    fun getScanMode() = ScanMode.from(preferences.scanMode!!)

    fun setScanMode(scanMode: ScanMode) =
        run {
            preferences.scanMode = scanMode.value
            _scanMode.value = scanMode
        }

    fun getScanModeFlag() = preferences.hasScanModeBeenChosen

    fun setScanModeFlag(value: Boolean) =
        run { preferences.hasScanModeBeenChosen = value }

    init {
        preferences.shouldInitDownload = false
        preferences.isDoubleScanFlow = false
        preferences.userName = ""

        fetchStatus.addSource(verifierRepository.getCertificateFetchStatus()) {
            fetchStatus.value = it
        }

        maxRetryReached.addSource(verifierRepository.getMaxRetryReached()) {
            maxRetryReached.value = it
        }
        sizeOverLiveData.addSource(verifierRepository.getSizeOverLiveData()) {
            sizeOverLiveData.value = it
        }

        initDownloadLiveData.addSource(verifierRepository.getInitDownloadLiveData()) {
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
    fun setDownloadAsAvailable() =
        run { preferences.authorizedToDownload = 1L }

    fun getResumeAvailable() = preferences.authToResume
    fun setResumeAsAvailable() =
        run { preferences.authToResume = 1L }

    fun getIsPendingDownload(): Boolean {
        return preferences.currentVersion != preferences.requestedVersion
    }

    fun getIsDrlSyncActive() = preferences.isDrlSyncActive

    fun setShouldInitDownload(value: Boolean) = run {
        preferences.shouldInitDownload = value
    }

    fun getCurrentChunk() = preferences.currentChunk

    fun getAppMinVersion(): String {
        return getRuleSet()?.getAppMinVersion() ?: ""
    }

    private fun getSDKMinVersion(): String {
        return getRuleSet()?.getSDKMinVersion() ?: ""
    }

    fun getRuleSet(): RuleSet? {
        return if (!preferences.validationRulesJson.isNullOrEmpty()) {
            RuleSet(preferences.validationRulesJson)
        } else null
    }

    fun isSDKVersionObsolete(): Boolean {
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

