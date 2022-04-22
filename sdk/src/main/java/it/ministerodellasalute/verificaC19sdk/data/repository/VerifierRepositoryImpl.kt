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
 *  Created by mykhailo.nester on 4/24/21 2:16 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import dgca.verifier.app.decoder.base64ToX509Certificate
import io.realm.Realm
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.where
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.local.realm.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.local.realm.RevokedPassEU
import it.ministerodellasalute.verificaC19sdk.data.local.room.AppDatabase
import it.ministerodellasalute.verificaC19sdk.data.local.room.Blacklist
import it.ministerodellasalute.verificaC19sdk.data.local.room.Key
import it.ministerodellasalute.verificaC19sdk.data.remote.ApiService
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CertificateRevocationList
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CrlStatus
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.DebugInfoWrapper
import it.ministerodellasalute.verificaC19sdk.model.DrlFlowType
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.security.KeyStoreCryptor
import it.ministerodellasalute.verificaC19sdk.util.ConversionUtility
import retrofit2.HttpException
import java.net.HttpURLConnection
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

    private lateinit var ruleSet: RuleSet
    private var crlstatus: CrlStatus? = null
    private val validCertList = mutableListOf<String>()
    private val fetchStatus: MutableLiveData<Boolean> = MutableLiveData()
    private val maxRetryReached: MutableLiveData<Boolean> = MutableLiveData()
    private val sizeOverLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private val initDownloadLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private val debugInfoLiveData: MutableLiveData<DebugInfoWrapper> = MutableLiveData()

    private lateinit var context: Context
    private var realmSize: Int? = null
    private var currentRetryNum: Int = 0

    override suspend fun syncData(applicationContext: Context): Boolean? {
        context = applicationContext

        return execute {
            fetchStatus.postValue(true)

            if (fetchValidationRules() == false || fetchCertificates() == false) {
                fetchStatus.postValue(false)
                return@execute false
            }

            updateDebugInfoWrapper()

            if (preferences.isDrlSyncActive) {
                getCRLStatus(DrlFlowType.IT.value)
                getCRLStatus(DrlFlowType.EU.value)
            }

            fetchStatus.postValue(false)
            preferences.dateLastFetch = System.currentTimeMillis()
            return@execute true
        }
    }

    private suspend fun fetchValidationRules(): Boolean? {
        return execute {
            val response = apiService.getValidationRules()
            val body = response.body() ?: run {
                return@execute false
            }
            preferences.validationRulesJson = body.stringSuspending(dispatcherProvider)
            ruleSet = RuleSet(preferences.validationRulesJson)
            val rules: Array<Rule> =
                Gson().fromJson(preferences.validationRulesJson, Array<Rule>::class.java)
            val listAsString: String =
                rules.find { it.name == ValidationRulesEnum.BLACK_LIST_UVCI.value }?.value?.trim()
                    ?: run {
                        ""
                    }
            db.blackListDao().deleteAll()
            listAsString.split(";").forEach {
                if (it.trim() != "") {
                    val blackListDto = Blacklist(it)
                    db.blackListDao().insert(blackListDto)
                }
            }
            preferences.isDrlSyncActive =
                rules.find { it.name == ValidationRulesEnum.DRL_SYNC_ACTIVE.name }
                    ?.let { ConversionUtility.stringToBoolean(it.value) } ?: true

            preferences.maxRetryNumber =
                rules.find { it.name == ValidationRulesEnum.MAX_RETRY.name }?.value?.toInt() ?: 1
            return@execute true
        }
    }

    private suspend fun fetchCertificates(): Boolean? {
        return execute {
            val response = apiService.getCertStatus()
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

    override suspend fun checkInBlackList(ucvi: String): Boolean {
        return try {
            db.blackListDao().getById(ucvi) != null
        } catch (e: Exception) {
            Log.i("BlackListException", e.localizedMessage ?: " ucvi not found in black list.")
            false
        }
    }

    override fun getMaxRetryReached(): LiveData<Boolean> {
        return maxRetryReached
    }

    override fun getSizeOverLiveData(): LiveData<Boolean> {
        return sizeOverLiveData
    }

    override fun getDebugInfoLiveData(): LiveData<DebugInfoWrapper> {
        return debugInfoLiveData
    }

    override fun resetCurrentRetryStatus() {
        currentRetryNum = 0
        maxRetryReached.value = false
    }

    private suspend fun fetchCertificate(resumeToken: Long): Boolean? {
        return execute {
            val tokenFormatted = if (resumeToken == -1L) "" else resumeToken.toString()
            val response = apiService.getCertUpdate(tokenFormatted)

            if (!response.isSuccessful) {
                return@execute false
            }

            if (response.isSuccessful && response.code() == HttpURLConnection.HTTP_OK) {
                val headers = response.headers()
                val responseKid = headers[HEADER_KID]
                val newResumeToken = headers[HEADER_RESUME_TOKEN]
                val responseStr =
                    response.body()?.stringSuspending(dispatcherProvider) ?: return@execute false

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

    private suspend fun getCRLStatus(drlFlowType: String) {
        try {
            if (isRetryAllowed()) {
                val responseIT = apiService.getCRLStatusIT(preferences.drlStateIT.currentVersion)
                val responseEU = apiService.getCRLStatusEU(preferences.drlStateEU.currentVersion)

                if (responseIT.isSuccessful && responseEU.isSuccessful) {
                    val crlstatusIT = Gson().fromJson(responseIT.body()?.string(), CrlStatus::class.java)
                    val crlstatusEU = Gson().fromJson(responseEU.body()?.string(), CrlStatus::class.java)

                    // TODO: add another preference for totalChunk = totalChunkIT + totalChunkEU.
                    preferences.drlStateIT.totalChunk = crlstatusIT.totalChunk + crlstatusEU.totalChunk
                    Log.i("CRL Status", Gson().toJson(crlstatus))

                    crlstatus = when (drlFlowType) {
                        DrlFlowType.IT.value -> crlstatusIT
                        DrlFlowType.EU.value -> crlstatusEU
                        else -> null
                    }

                    crlstatus?.let { crlStatus ->
                        if (isRetryAllowed()) {
                            if (outDatedVersion(crlStatus)) {
                                Log.i("outDatedVersion", "ok")
                                Log.i("noPendingDownload", noPendingDownload().toString())
                                if (noPendingDownload() || preferences.authorizedToDownload == 1L) {
                                    saveCrlStatusInfo(crlStatus)
                                    Log.i("SizeOver", isSizeOverThreshold(crlStatus).toString())
                                    if (isSizeOverThreshold(crlStatus) && !preferences.shouldInitDownload) {
                                        sizeOverLiveData.postValue(true)
                                    } else {
                                        sizeOverLiveData.postValue(false)
                                        downloadChunks(drlFlowType)
                                    }
                                } else {
                                    if (isSameChunkSize(crlStatus) && sameRequestedVersion(crlStatus)) {
                                        if (preferences.authToResume == 1L) downloadChunks(drlFlowType)
                                        else {
                                            Log.i(
                                                "atLeastOneChunk",
                                                atLeastOneChunkDownloaded().toString()
                                            )
                                            if (atLeastOneChunkDownloaded()) preferences.authToResume =
                                                0L
                                            else initDownloadLiveData.postValue(true)
                                        }
                                    } else {
                                        clearDBAndPrefs()
                                        this.syncData(context)
                                    }
                                }
                            } else {
                                persistLocalUCVINumber(crlStatus)
                                manageFinalReconciliation(drlFlowType)
                            }
                        } else {
                            maxRetryReached.postValue(true)
                        }
                    }
                } else {
                    throw HttpException(responseIT)
                }
            } else {
                maxRetryReached.postValue(true)
            }
        } catch (e: HttpException) {
            if (e.code() in 400..407) {
                Log.i(e.toString(), e.message())
                currentRetryNum++
                clearDBAndPrefs()
                preferences.shouldInitDownload = true
                this.syncData(context)
            } else {
                Log.i("StatusHttpException: $e", e.message())
            }
        }
    }

    override fun getInitDownloadLiveData(): LiveData<Boolean> {
        return initDownloadLiveData
    }

    private fun atLeastOneChunkDownloaded(): Boolean {
        return preferences.drlStateIT.currentChunk > 0 && preferences.drlStateIT.totalChunk > 0
    }

    private suspend fun manageFinalReconciliation(drlFlowType: String) {
        saveLastFetchDate()
        checkCurrentDownloadSize(drlFlowType)
        if (!isDownloadCompleted()) {
            Log.i("Reconciliation", "final reconciliation failed!")
            handleErrorState()
        } else Log.i("Reconciliation", "final reconciliation completed!")
    }

    private suspend fun handleErrorState() {
        currentRetryNum += 1
        clearDBAndPrefs()
        this.syncData(context)
    }

    private fun isRetryAllowed() = currentRetryNum < preferences.maxRetryNumber

    private fun saveCrlStatusInfo(crlStatus: CrlStatus) {
        persistLocalUCVINumber(crlStatus)
        preferences.drlStateIT = preferences.drlStateIT.apply {
            sizeSingleChunkInByte = crlStatus.sizeSingleChunkInByte
            requestedVersion = crlStatus.version
            currentVersion = crlStatus.fromVersion ?: 0L
            totalSizeInByte = crlStatus.totalSizeInByte
            chunk = crlStatus.chunk
        }
        preferences.authorizedToDownload = 0
    }

    private fun persistLocalUCVINumber(crlStatus: CrlStatus) {
        preferences.drlStateIT = preferences.drlStateIT.apply {
            totalNumberUCVI = crlStatus.totalNumberUCVI
        }
    }

    private fun checkCurrentDownloadSize(drlFlowType: String) {
        val realm: Realm = Realm.getDefaultInstance()
        realm.executeTransaction { transactionRealm ->
            val revokedPasses =
                when (drlFlowType) {
                    DrlFlowType.IT.value -> transactionRealm.where<RevokedPass>().findAll()
                    DrlFlowType.EU.value -> transactionRealm.where<RevokedPassEU>().findAll()
                    else -> transactionRealm.where<RevokedPass>().findAll()
                }
            realmSize = revokedPasses.size
            updateDebugInfoWrapper()
        }
        realm.close()
    }

    private fun isDownloadCompleted() = preferences.drlStateIT.totalNumberUCVI.toInt() == realmSize

    private suspend fun getRevokeList(version: Long, bodyResponse: String?, drlFlowType: String) {
        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            bodyResponse,
            CertificateRevocationList::class.java
        )
        if (version == certificateRevocationList.version) {
            preferences.drlStateIT = preferences.drlStateIT.apply {
                currentChunk += 1
            }
            val isFirstChunk = preferences.drlStateIT.currentChunk == 1L
            if (isFirstChunk && certificateRevocationList.delta == null) deleteAllFromRealm()
            persistRevokes(certificateRevocationList, drlFlowType)
        } else {
            clearDBAndPrefs()
            this.syncData(context)
        }
    }

    private fun persistRevokes(certificateRevocationList: CertificateRevocationList, drlFlowType: String) {
        try {
            val revokedUcviList = certificateRevocationList.revokedUcvi

            if (revokedUcviList != null) {
                Log.i("processRevokeList", " adding UCVI")
                insertListToRealm(revokedUcviList, drlFlowType)
            } else if (certificateRevocationList.delta != null) {
                Log.i("Delta", "delta")
                val deltaInsertList = certificateRevocationList.delta.insertions
                val deltaDeleteList = certificateRevocationList.delta.deletions

                if (deltaInsertList != null) {
                    Log.i("DeltaInsertions", "${deltaInsertList.size}")
                    insertListToRealm(deltaInsertList, drlFlowType)
                }
                if (deltaDeleteList != null) {
                    Log.i("DeltaDeletion", "${deltaDeleteList.size}")
                    deleteListFromRealm(deltaDeleteList, drlFlowType)
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
            Log.i("Cleared all data", "KO")
            preferences.clearDrlPrefs()
            deleteAllFromRealm()
            updateDebugInfoWrapper()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("ClearDBClearPreds", it)
            }
        }
    }

    private fun updateDebugInfoWrapper() {
        debugInfoLiveData.postValue(DebugInfoWrapper(validCertList, realmSize))
    }

    private fun noPendingDownload(): Boolean {
        return preferences.drlStateIT.currentVersion == preferences.drlStateIT.requestedVersion
    }

    private fun outDatedVersion(remoteStatus: CrlStatus): Boolean {
        return (remoteStatus.version != preferences.drlStateIT.currentVersion)
    }

    private fun sameRequestedVersion(crlStatus: CrlStatus): Boolean {
        return (crlStatus.version == preferences.drlStateIT.requestedVersion)
    }

    private fun isSizeOverThreshold(crlStatus: CrlStatus): Boolean {
        return (crlStatus.totalSizeInByte > ConversionUtility.megaByteToByte(5f))
    }

    private fun isSameChunkSize(crlStatus: CrlStatus): Boolean {
        return (preferences.drlStateIT.sizeSingleChunkInByte == crlStatus.sizeSingleChunkInByte)
    }

    override suspend fun downloadChunks(drlFlowType: String) {
        crlstatus?.let { status ->
            preferences.authToResume = -1
            while (noMoreChunks(status)) {
                try {
                    val response =
                        when (drlFlowType) {
                            DrlFlowType.IT.value -> apiService.getRevokeListIT(
                                preferences.drlStateIT.currentVersion,
                                preferences.drlStateIT.currentChunk + 1
                            )
                            DrlFlowType.EU.value -> apiService.getRevokeListEU(
                                preferences.drlStateEU.currentVersion,
                                preferences.drlStateEU.currentChunk + 1
                            )
                            else -> throw Exception("Unknown DrlFlowType")
                        }
                    if (response.isSuccessful) {
                        getRevokeList(status.version, response.body()?.string(), drlFlowType)
                    } else {
                        throw HttpException(response)
                    }
                } catch (e: HttpException) {
                    if (e.code() in 400..407) {
                        Log.i(e.toString(), e.message())
                        currentRetryNum++
                        clearDBAndPrefs()
                        preferences.shouldInitDownload = true
                        this.syncData(context)
                        break
                    } else {
                        Log.i("ChunkHttpException: $e", e.message())
                        break
                    }
                } catch (e: Exception) {
                    Log.i("ConnectionIssues", e.toString())
                    preferences.authToResume = 0
                    break
                }
            }
            if (isDownloadComplete(status)) {
                preferences.drlStateIT = preferences.drlStateIT.apply {
                    currentVersion = requestedVersion
                    currentChunk = 0
                    totalChunk = 0
                }
                preferences.authorizedToDownload = 1L
                preferences.authToResume = -1L
                preferences.shouldInitDownload = false
                getCRLStatus(drlFlowType)
                Log.i("chunk download", "Last chunk processed, versions updated")
            }
        }
    }

    private fun isDownloadComplete(status: CrlStatus) =
        preferences.drlStateIT.currentChunk == status.totalChunk

    private fun saveLastFetchDate() {
        preferences.drlStateIT = preferences.drlStateIT.apply {
            dateLastFetch = System.currentTimeMillis()
        }
    }

    private fun noMoreChunks(status: CrlStatus): Boolean =
        preferences.drlStateIT.currentChunk < status.totalChunk


    private fun insertListToRealm(deltaInsertList: MutableList<String>, drlFlowType: String) {
        try {
            val realm: Realm = Realm.getDefaultInstance()
            val revokesArrayIT: MutableList<RevokedPass> = mutableListOf()
            val revokesArrayEU: MutableList<RevokedPassEU> = mutableListOf()

            for (deltaInsert in deltaInsertList) {
                when (drlFlowType) {
                    DrlFlowType.IT.value -> revokesArrayIT.add(RevokedPass(deltaInsert))
                    DrlFlowType.EU.value -> revokesArrayEU.add(RevokedPassEU(deltaInsert))
                    else -> throw Exception("Unknown DrlFlowType")
                }
            }

            try {
                realm.executeTransaction { transactionRealm ->
                    when (drlFlowType) {
                        DrlFlowType.IT.value -> transactionRealm.insertOrUpdate(revokesArrayIT)
                        DrlFlowType.EU.value -> transactionRealm.insertOrUpdate(revokesArrayEU)
                    }
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

    private fun deleteAllFromRealm() {
        try {
            val realm: Realm = Realm.getDefaultInstance()

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

    private fun deleteListFromRealm(deltaDeleteList: MutableList<String>, drlFlowType: String) {
        try {
            val realm: Realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { transactionRealm ->
                    when (drlFlowType) {
                        DrlFlowType.IT.value -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPass>()
                                .`in`("hashedUVCI", deltaDeleteList.toTypedArray()).findAll()
                            Log.i("Revoke IT", revokedPassesToDelete.count().toString())
                            revokedPassesToDelete.deleteAllFromRealm()
                        }
                        DrlFlowType.EU.value -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPassEU>()
                                .`in`("hashedUVCI", deltaDeleteList.toTypedArray()).findAll()
                            Log.i("Revoke EU", revokedPassesToDelete.count().toString())
                            revokedPassesToDelete.deleteAllFromRealm()
                        }
                    }
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("DRL RealmPrimaryKeyConstraintException", it)
                }
            }
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("DRL Exception", it)
            }
        }
    }

    companion object {
        const val REALM_NAME = "VerificaC19"
        const val HEADER_KID = "x-kid"
        const val HEADER_RESUME_TOKEN = "x-resume-token"
    }

}

