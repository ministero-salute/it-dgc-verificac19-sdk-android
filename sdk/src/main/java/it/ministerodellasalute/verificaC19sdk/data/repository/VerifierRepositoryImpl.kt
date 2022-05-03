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
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.model.drl.DownloadState
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
    private val debugInfoLiveData: MutableLiveData<DebugInfoWrapper> = MutableLiveData()
    private val downloadStatus: MutableLiveData<DownloadState> = MutableLiveData()

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
            fetchStatus.postValue(false)
            return@execute true
        }
    }

    override fun setDownloadStatus(downloadStatus: DownloadState) {
        this.downloadStatus.postValue(downloadStatus)
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

    override fun setCertificateFetchStatus(fetchStatus: Boolean) = this.fetchStatus.postValue(fetchStatus)

    override fun setDebugInfoLiveData() = debugInfoLiveData.postValue(DebugInfoWrapper(validCertList, realmSize))

    override suspend fun checkInBlackList(ucvi: String): Boolean {
        return try {
            db.blackListDao().getById(ucvi) != null
        } catch (e: Exception) {
            Log.i("BlackListException", e.localizedMessage ?: "ucvi not found in black list.")
            false
        }
    }

    override fun getDebugInfoLiveData(): LiveData<DebugInfoWrapper> {
        return debugInfoLiveData
    }

    override fun resetCurrentRetryStatus() {
        currentRetryNum = 0
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

    override suspend fun callCRLStatus() {
        execute {
            getCRLStatus(DrlFlowType.IT)
            getCRLStatus(DrlFlowType.EU)
        }
    }

    private suspend fun getCRLStatus(drlFlowType: DrlFlowType) {
        try {
            if (isRetryAllowed()) {
                val responseIT = apiService.getCRLStatusIT(preferences.drlStateIT.currentVersion)
                val responseEU = apiService.getCRLStatusEU(preferences.drlStateEU.currentVersion)

                if (responseIT.isSuccessful && responseEU.isSuccessful) {
                    val crlStatusIT = Gson().fromJson(responseIT.body()?.string(), CrlStatus::class.java)
                    val crlStatusEU = Gson().fromJson(responseEU.body()?.string(), CrlStatus::class.java)

                    crlstatus = when (drlFlowType) {
                        DrlFlowType.IT -> crlStatusIT
                        DrlFlowType.EU -> crlStatusEU
                    }

                    crlstatus?.let { crlStatus ->
                        if (isRetryAllowed()) {
                            if (outDatedVersion(crlStatus, drlFlowType)) {
                                Log.i("outDatedVersion", "OK")
                                Log.i("noPendingDownload", noPendingDownload(drlFlowType).toString())
                                if (noPendingDownload(drlFlowType) || preferences.authorizedToDownload == 1L) {
                                    saveCrlStatusInfo(crlStatus, drlFlowType)
                                    Log.i("isSizeOverThreshold", isSizeOverThreshold().toString())
                                    if (isSizeOverThreshold() && !preferences.shouldInitDownload) {
                                        if (drlFlowType == DrlFlowType.IT) {
                                            downloadStatus.postValue(
                                                DownloadState.RequiresConfirm(
                                                    (crlStatusIT.totalSizeInByte +
                                                            crlStatusEU.totalSizeInByte).toFloat()
                                                )
                                            )
                                        }
                                    } else {
                                        downloadChunks(drlFlowType)
                                    }
                                } else {
                                    if (isSameChunkSize(crlStatus, drlFlowType) && sameRequestedVersion(crlStatus, drlFlowType)) {
                                        if (preferences.authToResume == 1L) downloadChunks(drlFlowType)
                                        else {
                                            if (atLeastOneChunkDownloaded()) downloadStatus.postValue(DownloadState.ResumeAvailable)
                                            else downloadStatus.postValue(DownloadState.DownloadAvailable)
                                        }
                                    } else {
                                        clearDBAndPrefs(drlFlowType)
                                        getCRLStatus(drlFlowType)
                                    }
                                }
                            } else {
                                persistLocalUCVINumber(crlStatus, drlFlowType)
                                manageFinalReconciliation(drlFlowType)
                            }
                        } else {
                            downloadStatus.postValue(DownloadState.DownloadAvailable)
                        }
                    }
                } else {
                    throw HttpException(responseIT)
                }
            } else {
                downloadStatus.postValue(DownloadState.DownloadAvailable)
            }
        } catch (e: HttpException) {
            if (e.code() in 400..407) {
                Log.i(e.toString(), e.message())
                currentRetryNum++
                clearDBAndPrefs(drlFlowType)
                preferences.shouldInitDownload = true
                getCRLStatus(drlFlowType)
            } else {
                Log.i("StatusHttpException: $e", e.message())
            }
        }
    }

    private fun atLeastOneChunkDownloaded(): Boolean {
        return ((preferences.drlStateIT.currentChunk + preferences.drlStateEU.currentChunk) > 0)
    }

    private suspend fun manageFinalReconciliation(drlFlowType: DrlFlowType) {
        saveLastFetchDate(drlFlowType)
        checkCurrentDownloadSize(drlFlowType)
        if (!isDownloadCompleted(drlFlowType)) {
            Log.i("Final reconciliation", "failed!")
            handleErrorState(drlFlowType)
        } else {
            Log.i("Final reconciliation complete for: ", drlFlowType.value)
            if (drlFlowType == DrlFlowType.EU) {
                downloadStatus.postValue(DownloadState.Complete)
                preferences.authorizedToDownload = 1L
                preferences.authToResume = -1L
                preferences.shouldInitDownload = false
            }
        }
    }

    private suspend fun handleErrorState(drlFlowType: DrlFlowType) {
        currentRetryNum += 1
        clearDBAndPrefs(drlFlowType)
        getCRLStatus(drlFlowType)
    }

    private fun isRetryAllowed() = currentRetryNum < preferences.maxRetryNumber

    private fun saveCrlStatusInfo(crlStatus: CrlStatus, drlFlowType: DrlFlowType) {
        persistLocalUCVINumber(crlStatus, drlFlowType)
        when (drlFlowType) {
            DrlFlowType.IT -> {
                preferences.drlStateIT = preferences.drlStateIT.apply {
                    sizeSingleChunkInByte = crlStatus.sizeSingleChunkInByte
                    requestedVersion = crlStatus.version
                    totalChunk = crlStatus.totalChunk
                    currentVersion = crlStatus.fromVersion ?: 0L
                    totalSizeInByte = crlStatus.totalSizeInByte
                    chunk = crlStatus.chunk
                }
            }
            DrlFlowType.EU -> {
                preferences.drlStateEU = preferences.drlStateEU.apply {
                    sizeSingleChunkInByte = crlStatus.sizeSingleChunkInByte
                    requestedVersion = crlStatus.version
                    totalChunk = crlStatus.totalChunk
                    currentVersion = crlStatus.fromVersion ?: 0L
                    totalSizeInByte = crlStatus.totalSizeInByte
                    chunk = crlStatus.chunk
                }
            }
        }
        preferences.authorizedToDownload = 0
    }

    private fun persistLocalUCVINumber(crlStatus: CrlStatus, drlFlowType: DrlFlowType) {
        when (drlFlowType) {
            DrlFlowType.IT -> {
                preferences.drlStateIT = preferences.drlStateIT.apply {
                    totalNumberUCVI = crlStatus.totalNumberUCVI
                }
            }
            DrlFlowType.EU -> {
                preferences.drlStateEU = preferences.drlStateEU.apply {
                    totalNumberUCVI = crlStatus.totalNumberUCVI
                }
            }
        }
    }

    private fun checkCurrentDownloadSize(drlFlowType: DrlFlowType) {
        val realm: Realm = Realm.getDefaultInstance()
        realm.executeTransaction { transactionRealm ->
            val revokedPasses =
                when (drlFlowType) {
                    DrlFlowType.IT -> transactionRealm.where<RevokedPass>().findAll()
                    DrlFlowType.EU -> transactionRealm.where<RevokedPassEU>().findAll()
                }
            realmSize = revokedPasses.size
            updateDebugInfoWrapper()
        }
        realm.close()
    }

    private fun isDownloadCompleted(drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> preferences.drlStateIT.totalNumberUCVI.toInt() == realmSize
            DrlFlowType.EU -> preferences.drlStateEU.totalNumberUCVI.toInt() == realmSize
        }
    }

    private suspend fun getRevokeList(version: Long, bodyResponse: String?, drlFlowType: DrlFlowType) {
        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            bodyResponse,
            CertificateRevocationList::class.java
        )
        if (version == certificateRevocationList.version) {

            var isFirstChunk = true
            when (drlFlowType) {
                DrlFlowType.IT -> {
                    preferences.drlStateIT = preferences.drlStateIT.apply {
                        currentChunk += 1
                    }
                    isFirstChunk = preferences.drlStateIT.currentChunk == 1L
                }
                DrlFlowType.EU -> {
                    preferences.drlStateEU = preferences.drlStateEU.apply {
                        currentChunk += 1
                    }
                    isFirstChunk = preferences.drlStateEU.currentChunk == 1L
                }
            }

            if (isFirstChunk && certificateRevocationList.delta == null) deleteAllFromRealm(drlFlowType)
            persistRevokes(certificateRevocationList, drlFlowType)
        } else {
            clearDBAndPrefs(drlFlowType)
            getCRLStatus(drlFlowType)
        }
    }

    private fun persistRevokes(certificateRevocationList: CertificateRevocationList, drlFlowType: DrlFlowType) {
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
                Log.i("CRL processing exception", it)
            }
        }

    }

    private fun clearDBAndPrefs(drlFlowType: DrlFlowType) {
        try {
            preferences.clearDrlPrefs()
            deleteAllFromRealm(drlFlowType)
            updateDebugInfoWrapper()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("clearDBAndPrefs", it)
            }
        }
    }

    private fun updateDebugInfoWrapper() {
        debugInfoLiveData.postValue(DebugInfoWrapper(validCertList, realmSize))
    }

    private fun noPendingDownload(drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> preferences.drlStateIT.currentVersion == preferences.drlStateIT.requestedVersion
            DrlFlowType.EU -> preferences.drlStateEU.currentVersion == preferences.drlStateEU.requestedVersion
        }
    }

    private fun outDatedVersion(remoteStatus: CrlStatus, drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> (remoteStatus.version != preferences.drlStateIT.currentVersion)
            DrlFlowType.EU -> (remoteStatus.version != preferences.drlStateEU.currentVersion)
        }
    }

    private fun sameRequestedVersion(crlStatus: CrlStatus, drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> (crlStatus.version == preferences.drlStateIT.requestedVersion)
            DrlFlowType.EU -> (crlStatus.version == preferences.drlStateEU.requestedVersion)
        }
    }

    private fun isSizeOverThreshold(): Boolean {
        return (preferences.drlStateEU.totalSizeInByte + preferences.drlStateIT.totalSizeInByte) > ConversionUtility.megaByteToByte(5f)
    }

    private fun isSameChunkSize(crlStatus: CrlStatus, drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> (preferences.drlStateIT.sizeSingleChunkInByte == crlStatus.sizeSingleChunkInByte)
            DrlFlowType.EU -> (preferences.drlStateEU.sizeSingleChunkInByte == crlStatus.sizeSingleChunkInByte)
        }
    }

    override suspend fun downloadChunks(drlFlowType: DrlFlowType) {
        crlstatus?.let { status ->
            preferences.authToResume = -1
            while (noMoreChunks(status, drlFlowType)) {
                downloadStatus.postValue(DownloadState.Downloading)
                try {
                    val response =
                        when (drlFlowType) {
                            DrlFlowType.IT -> apiService.getRevokeListIT(
                                preferences.drlStateIT.currentVersion,
                                preferences.drlStateIT.currentChunk + 1
                            )
                            DrlFlowType.EU -> apiService.getRevokeListEU(
                                preferences.drlStateEU.currentVersion,
                                preferences.drlStateEU.currentChunk + 1
                            )
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
                        clearDBAndPrefs(drlFlowType)
                        preferences.shouldInitDownload = true
                        getCRLStatus(drlFlowType)
                        break
                    } else {
                        Log.i("ChunkHttpException: $e", e.message())
                        break
                    }
                } catch (e: Exception) {
                    Log.i("ConnectionIssues", e.toString())
                    downloadStatus.postValue(DownloadState.ResumeAvailable)
                    break
                }
            }
            if (isChunkDownloadComplete(status, drlFlowType)) {
                when (drlFlowType) {
                    DrlFlowType.IT -> {
                        preferences.drlStateIT = preferences.drlStateIT.apply {
                            currentVersion = requestedVersion
                        }
                    }
                    DrlFlowType.EU -> {
                        preferences.drlStateEU = preferences.drlStateEU.apply {
                            currentVersion = requestedVersion
                        }
                    }
                }
                preferences.authorizedToDownload = 1L
                preferences.authToResume = -1L
                getCRLStatus(drlFlowType)
                Log.i("Chunk download", "last chunk processed - versions updated.")
            }
        }
    }

    private fun isChunkDownloadComplete(status: CrlStatus, drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> preferences.drlStateIT.currentChunk == status.totalChunk
            DrlFlowType.EU -> preferences.drlStateEU.currentChunk == status.totalChunk
        }
    }

    private fun saveLastFetchDate(drlFlowType: DrlFlowType) {
        return when (drlFlowType) {
            DrlFlowType.IT -> preferences.drlStateIT = preferences.drlStateIT.apply {
                dateLastFetch = System.currentTimeMillis()
            }
            DrlFlowType.EU -> preferences.drlStateEU = preferences.drlStateEU.apply {
                dateLastFetch = System.currentTimeMillis()
            }
        }

    }

    override fun getDownloadStatusLiveData() = downloadStatus

    private fun noMoreChunks(status: CrlStatus, drlFlowType: DrlFlowType): Boolean {
        return when (drlFlowType) {
            DrlFlowType.IT -> preferences.drlStateIT.currentChunk < status.totalChunk
            DrlFlowType.EU -> preferences.drlStateEU.currentChunk < status.totalChunk
        }
    }

    private fun insertListToRealm(deltaInsertList: MutableList<String>, drlFlowType: DrlFlowType) {
        try {
            val realm: Realm = Realm.getDefaultInstance()
            val revokesArrayIT: MutableList<RevokedPass> = mutableListOf()
            val revokesArrayEU: MutableList<RevokedPassEU> = mutableListOf()

            for (deltaInsert in deltaInsertList) {
                when (drlFlowType) {
                    DrlFlowType.IT -> revokesArrayIT.add(RevokedPass(deltaInsert))
                    DrlFlowType.EU -> revokesArrayEU.add(RevokedPassEU(deltaInsert))
                }
            }

            try {
                realm.executeTransaction { transactionRealm ->
                    when (drlFlowType) {
                        DrlFlowType.IT -> transactionRealm.insertOrUpdate(revokesArrayIT)
                        DrlFlowType.EU -> transactionRealm.insertOrUpdate(revokesArrayEU)
                    }
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("RealmPrimaryKeyConstraintException", it)
                }
            }
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("RealmException", it)
            }
        }
    }

    private fun deleteAllFromRealm(drlFlowType: DrlFlowType) {
        try {
            val realm: Realm = Realm.getDefaultInstance()

            try {
                realm.executeTransaction { transactionRealm ->
                    when (drlFlowType) {
                        DrlFlowType.IT -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPass>().findAll()
                            Log.i("RevokesIT", revokedPassesToDelete.count().toString())
                            revokedPassesToDelete.deleteAllFromRealm()
                        }
                        DrlFlowType.EU -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPassEU>().findAll()
                            Log.i("RevokesEU", revokedPassesToDelete.count().toString())
                            revokedPassesToDelete.deleteAllFromRealm()
                        }
                    }
                }
            } catch (e: RealmPrimaryKeyConstraintException) {
                e.localizedMessage?.let {
                    Log.i("RealmPrimaryKeyConstraintException", it)
                }
            }
            realm.close()
        } catch (e: Exception) {
            e.localizedMessage?.let {
                Log.i("RealmException", it)
            }
        }
    }

    private fun deleteListFromRealm(deltaDeleteList: MutableList<String>, drlFlowType: DrlFlowType) {
        try {
            val realm: Realm = Realm.getDefaultInstance()
            try {
                realm.executeTransaction { transactionRealm ->
                    when (drlFlowType) {
                        DrlFlowType.IT -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPass>()
                                .`in`("hashedUVCI", deltaDeleteList.toTypedArray()).findAll()
                            Log.i("RevokesIT", revokedPassesToDelete.count().toString())
                            revokedPassesToDelete.deleteAllFromRealm()
                        }
                        DrlFlowType.EU -> {
                            val revokedPassesToDelete = transactionRealm.where<RevokedPassEU>()
                                .`in`("hashedUVCI", deltaDeleteList.toTypedArray()).findAll()
                            Log.i("RevokesEU", revokedPassesToDelete.count().toString())
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

