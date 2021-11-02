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
 *  Created by mykhailo.nester on 4/24/21 2:16 PM
 */

package it.ministerodellasalute.verificaC19sdk.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dgca.verifier.app.decoder.base64ToX509Certificate
import dgca.verifier.app.decoder.toBase64
import it.ministerodellasalute.verificaC19sdk.data.local.AppDatabase
import it.ministerodellasalute.verificaC19sdk.data.local.Blacklist
import it.ministerodellasalute.verificaC19sdk.data.local.Key
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.remote.ApiService
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.security.KeyStoreCryptor
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Response
import java.net.HttpURLConnection
import java.security.MessageDigest
import java.security.cert.Certificate
import javax.inject.Inject

/**
 *
 * This class contains several methods to download public certificates (i.e. settings) and check
 * the download status. It implements the interface [VerifierRepository].
 *
 */
class VerifierRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val preferences: Preferences,
    private val db: AppDatabase,
    private val keyStoreCryptor: KeyStoreCryptor,
    private val dispatcherProvider: DispatcherProvider
) : BaseRepository(dispatcherProvider), VerifierRepository {

    private val validCertList = mutableListOf<String>()
    private val fetchStatus: MutableLiveData<Boolean> = MutableLiveData()

    override suspend fun syncData(): Boolean? {
        return execute {
            fetchStatus.postValue(true)

            if (fetchValidationRules() == false || fetchCertificates() == false) {
                fetchStatus.postValue(false)
                return@execute false
            }

            fetchStatus.postValue(false)

            if (preferences.isClockAligned == false) {
                preferences.dateLastFetch = -1L
            } else {
                preferences.dateLastFetch = System.currentTimeMillis()
            }
            return@execute true
        }
    }

    private suspend fun fetchValidationRules(): Boolean? {
        return execute {
            val response = apiService.getValidationRules()

            val headers = response.headers()

            if (checkClockAlignment(headers) == false) {
                return@execute false
            }

            val body = response.body() ?: run {
                return@execute false
            }
            preferences.validationRulesJson = body.stringSuspending(dispatcherProvider)
            var jsonBlackList = Gson().fromJson(preferences.validationRulesJson, Array<Rule>::class.java)
            var listasString = jsonBlackList.find { it.name == ValidationRulesEnum.BLACK_LIST_UVCI.value }?.let {
                it.value.trim()
            } ?: run {
                ""
            }

            db.blackListDao().deleteAll()
            val list_blacklist = listasString.split(";")
            for (blacklist_item in list_blacklist)
            {
                if (blacklist_item != null && blacklist_item.trim() != "") {
                    var blacklist_object = Blacklist(blacklist_item)
                    db.blackListDao().insert(blacklist_object)
                }
            }
            return@execute true
        }
    }

    private suspend fun fetchCertificates(): Boolean? {
        return execute {

            val response = apiService.getCertStatus()

            val headers = response.headers()

            if (checkClockAlignment(headers) == false) {
                return@execute false
            }

            val body = response.body() ?: run {
                return@execute false
            }
            validCertList.clear()
            validCertList.addAll(body)

            val recordCount = db.keyDao().getCount()
            if (body.isEmpty() || recordCount.equals(0)) {
                preferences.resumeToken = -1L
            }

            val resumeToken = preferences.resumeToken
            if (fetchCertificate(resumeToken) == false) {
                return@execute false
            }
            db.keyDao().deleteAllExcept(validCertList.toTypedArray())

            return@execute true
        }
    }

    override suspend fun getCertificate(kid: String): Certificate? {
        val key = db.keyDao().getById(kid)
        return if (key != null) keyStoreCryptor.decrypt(key.key)!!
            .base64ToX509Certificate() else null
    }

    override fun getCertificateFetchStatus(): LiveData<Boolean> {
        return fetchStatus
    }

    override suspend fun checkInBlackList(ucvi: String): Boolean
    {
        return try {
            db.blackListDao().getById(ucvi) != null
        } catch (e: Exception) {
            Log.i("TAG", e.localizedMessage)
            false
        }
    }

    private suspend fun fetchCertificate(resumeToken: Long): Boolean? {
        return execute {
            val tokenFormatted = if (resumeToken == -1L) "" else resumeToken.toString()
            val response = apiService.getCertUpdate(tokenFormatted)

            val headers = response.headers()

            if (checkClockAlignment(headers) == false) {
                return@execute false
            }

            if (!response.isSuccessful) {
                return@execute false
            }

            if (response.isSuccessful && response.code() == HttpURLConnection.HTTP_OK) {
                val responseKid = headers[HEADER_KID]
                val newResumeToken = headers[HEADER_RESUME_TOKEN]
                val responseStr = response.body()?.stringSuspending(dispatcherProvider) ?: return@execute false

                if (validCertList.contains(responseKid)) {
                    Log.i(VerifierRepositoryImpl::class.java.simpleName, "Cert KID verified")
                    val key = Key(kid = responseKid!!, key = keyStoreCryptor.encrypt(responseStr)!!)
                    db.keyDao().insert(key)

                    preferences.resumeToken = resumeToken

                    newResumeToken?.let {
                        val newToken = it.toLong()
                        fetchCertificate(newToken)
                    }
                }
            }
            return@execute true
        }
    }

    private suspend fun checkClockAlignment(headers: Headers): Boolean? {
        return execute {
            preferences.isClockAligned = true

            val responseClockSync = headers[HEADER_CLOCK_SYNC]

            if (responseClockSync == "falso") {
                preferences.isClockAligned = false
                preferences.dateLastFetch = -1L
                return@execute false
            }

        return@execute true
        }
    }

    companion object {

        const val HEADER_KID = "x-kid"
        const val HEADER_RESUME_TOKEN = "x-resume-token"
        const val HEADER_CLOCK_SYNC = "x-clock-sync"
    }

}

