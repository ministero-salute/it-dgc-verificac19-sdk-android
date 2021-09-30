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

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.util.Utility
import javax.inject.Inject

@HiltViewModel
class FirstViewModel @Inject constructor(
    verifierRepository: VerifierRepository,
    private val preferences: Preferences
) : ViewModel(){

    val fetchStatus: MediatorLiveData<Boolean> = MediatorLiveData()

    init {
        fetchStatus.addSource(verifierRepository.getCertificateFetchStatus()) {
            fetchStatus.value = it
        }
    }

    fun getDateLastSync() = preferences.dateLastFetch

    private fun getValidationRules():Array<Rule>{
        val jsonString = preferences.validationRulesJson
        return Gson().fromJson(jsonString, Array<Rule>::class.java)?: kotlin.run { emptyArray() }
    }

    fun getAppMinVersion(): String{
        return getValidationRules().find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value}?.let {
            it.value
        } ?: run {
            ""
        }
    }

    private fun getSDKMinVersion(): String{
        return getValidationRules().find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value}?.let {
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
}