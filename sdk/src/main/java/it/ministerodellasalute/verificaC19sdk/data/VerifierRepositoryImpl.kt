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

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dgca.verifier.app.decoder.base64ToX509Certificate
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.where
import it.ministerodellasalute.verificaC19sdk.data.local.AppDatabase
import it.ministerodellasalute.verificaC19sdk.data.local.Key
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.local.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.remote.ApiService
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CertificateRevocationList
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CrlStatus
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.security.KeyStoreCryptor
import it.ministerodellasalute.verificaC19sdk.util.ConversionUtility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.http.HTTP
import java.io.IOException
import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.security.cert.Certificate
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

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

    private var crlstatus: CrlStatus? = null
    private val validCertList = mutableListOf<String>()
    private val fetchStatus: MutableLiveData<Boolean> = MutableLiveData()
    private val maxRetryReached: MutableLiveData<Boolean> = MutableLiveData()

    private lateinit var context: Context
    private var realmSize: Int = 0
    private var currentRetryNum: Int = 0

    override suspend fun syncData(applicationContext: Context): Boolean? {
        context = applicationContext
        Realm.init(applicationContext)

        return execute {
            fetchStatus.postValue(true)
            fetchValidationRules()
            val jsonString = preferences.validationRulesJson
            val validationRules = Gson().fromJson(jsonString, Array<Rule>::class.java)

            if (fetchCertificates() == false) {
                fetchStatus.postValue(false)
                return@execute false
            }

            validationRules.let {
                for (rule in validationRules) {
                    if (rule.name == "DRL_SYNC_ACTIVE") {
                        preferences.isDrlSyncActive = ConversionUtility.stringToBoolean(rule.value)
                        break
                    }
                    if (rule.name == "MAX_RETRY") {
                        preferences.maxRetryNumber = rule.value.toInt()
                        break
                    }
                }
            }
            if (preferences.isDrlSyncActive) {
                getCRLStatus()
            }

            fetchStatus.postValue(false)
            return@execute true
        }
    }

    private suspend fun fetchValidationRules() {
        val response = apiService.getValidationRules()
        val body = response.body() ?: run {
            return
        }
        preferences.validationRulesJson = body.stringSuspending(dispatcherProvider)
    }

    private suspend fun fetchCertificates(): Boolean? {
        return execute {

            val response = apiService.getCertStatus()
            val body = response.body() ?: run {
                return@execute false
            }
            validCertList.clear()
            validCertList.addAll(body)

            if (body.isEmpty()) {
                preferences.resumeToken = -1L
            }

            val resumeToken = preferences.resumeToken
            fetchCertificate(resumeToken)
            db.keyDao().deleteAllExcept(validCertList.toTypedArray())

            //if db is empty for a reason, refresh sharedprefs and DB
            val recordCount = db.keyDao().getCount()
            Log.i("record count", recordCount.toString())
            if (recordCount.equals(0)) {
                preferences.clear()
                this.syncData(context)
            }

            preferences.dateLastFetch = System.currentTimeMillis()

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

    override fun getMaxRetryReached(): LiveData<Boolean> {
        return maxRetryReached
    }

    override fun resetCurrentRetryStatus() {
        currentRetryNum = 0
        maxRetryReached.value = false
    }

    private suspend fun fetchCertificate(resumeToken: Long) {
        val tokenFormatted = if (resumeToken == -1L) "" else resumeToken.toString()
        val response = apiService.getCertUpdate(tokenFormatted)

        if (response.isSuccessful && response.code() == HttpURLConnection.HTTP_OK) {
            val headers = response.headers()
            val responseKid = headers[HEADER_KID]
            val newResumeToken = headers[HEADER_RESUME_TOKEN]
            val responseStr = response.body()?.stringSuspending(dispatcherProvider) ?: return

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
    }

    override suspend fun isDrlInconsistent(): Boolean {
        val response = apiService.getCRLStatus(preferences.currentVersion)
        if (response.isSuccessful) {
            val status = Gson().fromJson(response.body()?.string(), CrlStatus::class.java)
            return outDatedVersion(status)
        }
        return false
    }

    private suspend fun getCRLStatus() {
        val response = apiService.getCRLStatus(preferences.currentVersion)
        if (response.isSuccessful) {
            crlstatus = Gson().fromJson(response.body()?.string(), CrlStatus::class.java)
            Log.i("CRL Status", crlstatus.toString())

            crlstatus?.let { crlStatus ->
                if (isRetryAllowed()) {
                    if (outDatedVersion(crlStatus)) {
                        if (noPendingDownload() || preferences.authorizedToDownload == 1L) {
                            saveCrlStatusInfo(crlStatus)
                            if (isSizeOverThreshold(crlStatus) && preferences.authorizedToDownload == 0L && !preferences.shouldInitDownload) {
                                preferences.isSizeOverThreshold = true
                            } else {
                                preferences.shouldInitDownload = false
                                downloadChunk()
                            }
                        } else if (preferences.authToResume == 1L) {
                            if (isSameChunkSize(crlStatus) && sameRequestedVersion(crlStatus)) downloadChunk()
                            else {
                                clearDBAndPrefs()
                                this.syncData(context)
                            }
                        } else {
                            //preferences.authToResume = 0L
                        }
                    } else {
                        saveLastFetchDate()
                        checkCurrentDownloadSize()
                        if (!isDownloadCompleted()) {
                            Log.i("MyTag", "final reconciliation failed!")
                            currentRetryNum += 1
                            clearDBAndPrefs()
                            this.syncData(context)
                        } else Log.i("MyTag", "final reconciliation completed!")
                    }
                } else {
                    maxRetryReached.postValue(true)
                }

            }
        }
    }

    private fun isRetryAllowed() = currentRetryNum < preferences.maxRetryNumber

    private fun saveCrlStatusInfo(crlStatus: CrlStatus) {
        preferences.sizeSingleChunkInByte = crlStatus.sizeSingleChunkInByte
        preferences.totalChunk = crlStatus.totalChunk
        preferences.requestedVersion = crlStatus.version
        preferences.currentVersion = crlStatus.fromVersion ?: 0L
        preferences.totalSizeInByte = crlStatus.totalSizeInByte
        preferences.chunk = crlStatus.chunk
        preferences.totalNumberUCVI = crlStatus.totalNumberUCVI
        preferences.authorizedToDownload = 0
    }

    private fun checkCurrentDownloadSize() {
        val config =
            RealmConfiguration.Builder().name(REALM_NAME).allowQueriesOnUiThread(true)
                .build()
        val realm: Realm = Realm.getInstance(config)
        realm.executeTransaction { transactionRealm ->
            realmSize = transactionRealm.where<RevokedPass>().findAll().size
        }
        realm.close()
    }

    private fun isDownloadCompleted() = preferences.totalNumberUCVI.toInt() == realmSize

    private suspend fun getRevokeList(version: Long, bodyResponse: String?) {

        /*val response =
                apiService.getRevokeList(preferences.currentVersion, chunk)*/
        //if (response.isSuccessful) {
        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            bodyResponse,
            CertificateRevocationList::class.java
        )
        if (version == certificateRevocationList.version) {
            preferences.currentChunk = preferences.currentChunk + 1
            val isFirstChunk = preferences.currentChunk == 1L
            if (isFirstChunk && certificateRevocationList.delta == null) deleteAllFromRealm()
            processRevokeList(certificateRevocationList)
        } else {
            clearDBAndPrefs()
            this.syncData(context)
        }
        //}
    }

    private fun processRevokeList(certificateRevocationList: CertificateRevocationList) {
        try {
            val revokedUcviList = certificateRevocationList.revokedUcvi

            if (revokedUcviList != null) {
                Log.i("processRevokeList", " adding UCVI")
                insertListToRealm(revokedUcviList)
            } else if (certificateRevocationList.delta != null) {
                Log.i("Delta", "delta")
                val deltaInsertList = certificateRevocationList.delta.insertions
                val deltaDeleteList = certificateRevocationList.delta.deletions

                if (deltaInsertList != null) {
                    Log.i("Delta", "delta insert")
                    insertListToRealm(deltaInsertList)
                }
                if (deltaDeleteList != null) {
                    Log.i("Delta", "delta delete")
                    deleteListFromRealm(deltaDeleteList)
                }
            }
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("crl processing exception", it)
            }
        }

    }

    private fun clearDBAndPrefs() {
        try {
            preferences.clearDrlPrefs()
            deleteAllFromRealm()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("ClearDBClearPreds", it)
            }
        }
    }

    private fun noPendingDownload(): Boolean {
        return preferences.currentVersion == preferences.requestedVersion
    }

    private fun outDatedVersion(crlStatus: CrlStatus): Boolean {
        return (crlStatus.version != preferences.currentVersion)
    }

    private fun sameRequestedVersion(crlStatus: CrlStatus): Boolean {
        return (crlStatus.version == preferences.requestedVersion)
    }

    private fun isSizeOverThreshold(crlStatus: CrlStatus): Boolean {
        return (crlStatus.totalSizeInByte > 5000000)
    }

    private fun isSameChunkSize(crlStatus: CrlStatus): Boolean {
        return (preferences.sizeSingleChunkInByte == crlStatus.sizeSingleChunkInByte)
    }

    override suspend fun downloadChunk() {
        crlstatus?.let { status ->
            //preferences.authorizedToDownload = 1
            preferences.authToResume = -1

            while (noMoreChunks(status)) {
                try {
                    val response =
                        apiService.getRevokeList(
                            preferences.currentVersion,
                            preferences.currentChunk + 1
                        )
                    Log.i("MyTag", response.code().toString())
                    if (response.isSuccessful) {
                        getRevokeList(status.version, response.body()?.string())
                    } else {
                        throw HttpException(response)
                    }
                } catch (e: HttpException) {
                    if (e.code() in 400..407) {
                        Log.i(e.toString(), e.message())
                        clearDBAndPrefs()
                        this.syncData(context)
                        break
                    } else {
                        Log.i("MyTag: $e", e.message())
                        break
                    }
                } catch (e: CancellationException) {
                    Log.i("MyTag: $e", e.cause.toString())
                    preferences.authToResume = 0
                    break
                }
            }
            if (isDownloadComplete(status)) {
                preferences.currentVersion = preferences.requestedVersion
                preferences.currentChunk = 0
                preferences.totalChunk = 0
                saveLastFetchDate()
                Log.i("chunk download", "Last chunk processed, versions updated")
            }
        }
    }

    private fun isDownloadComplete(status: CrlStatus) =
        preferences.currentChunk == status.totalChunk

    private fun saveLastFetchDate() {
        preferences.drlDateLastFetch = System.currentTimeMillis()
    }

    private fun noMoreChunks(status: CrlStatus): Boolean =
        preferences.currentChunk < status.totalChunk


    private fun insertListToRealm(deltaInsertList: MutableList<String>) {
        try {
            val config =
                RealmConfiguration.Builder().name(REALM_NAME).allowWritesOnUiThread(true).build()
            val realm: Realm = Realm.getInstance(config)
            val array = mutableListOf<RevokedPass>()

            for (deltaInsert in deltaInsertList) {
                array.add(RevokedPass(deltaInsert))
            }

            try {
                realm.executeTransaction { transactionRealm ->
                    transactionRealm.insertOrUpdate(array)
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("Revoke exc", it)
                }
            }
            Log.i("Revoke", "Inserted")
            val count = realm.where<RevokedPass>().findAll().size
            Log.i("Revoke", "Inserted $count")
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("Revoke exc2", it)
            }
        }
    }

    private fun deleteAllFromRealm() {
        try {
            val config =
                RealmConfiguration.Builder().name(REALM_NAME).allowWritesOnUiThread(true).build()
            val realm: Realm = Realm.getInstance(config)

            try {
                realm.executeTransaction { transactionRealm ->
                    transactionRealm.deleteAll()
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("Revoke exc", it)
                }
            }
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("Revoke exc2", it)
            }
        }
    }

    private fun deleteListFromRealm(deltaDeleteList: MutableList<String>) {
        try {
            val config =
                RealmConfiguration.Builder().name(REALM_NAME).allowWritesOnUiThread(true).build()
            val realm: Realm = Realm.getInstance(config)
            try {
                realm.executeTransaction { transactionRealm ->
                    var count = transactionRealm.where<RevokedPass>().findAll().size
                    Log.i("Revoke", "Before delete $count")
                    val revokedPassesToDelete = transactionRealm.where<RevokedPass>()
                        .`in`("hashedUVCI", deltaDeleteList.toTypedArray()).findAll()
                    Log.i("Revoke", revokedPassesToDelete.count().toString())
                    revokedPassesToDelete.deleteAllFromRealm()
                    count = transactionRealm.where<RevokedPass>().findAll().size
                    Log.i("Revoke", "After delete $count")
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("Revoke exc", it)
                }
            }
            val count = realm.where<RevokedPass>().findAll().size
            Log.i("Revoke", "deleted $count")
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("Revoke exc2", it)
            }
        }
    }

    companion object {
        const val REALM_NAME = "VerificaC19"
        const val HEADER_KID = "x-kid"
        const val HEADER_RESUME_TOKEN = "x-resume-token"
    }

}

