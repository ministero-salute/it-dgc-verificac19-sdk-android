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
import it.ministerodellasalute.verificaC19sdk.data.local.AppDatabase
import it.ministerodellasalute.verificaC19sdk.data.local.Key
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.remote.ApiService
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CertificateRevocationList
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CrlStatus
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.security.KeyStoreCryptor
import java.lang.Exception
import java.net.HttpURLConnection
import java.security.cert.Certificate
import javax.inject.Inject
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.exceptions.RealmException
import io.realm.exceptions.RealmPrimaryKeyConstraintException
import io.realm.kotlin.delete
import io.realm.kotlin.where
import it.ministerodellasalute.verificaC19sdk.data.local.RevokedPass


class VerifierRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val preferences: Preferences,
    private val db: AppDatabase,
    private val keyStoreCryptor: KeyStoreCryptor,
    private val dispatcherProvider: DispatcherProvider
) : BaseRepository(dispatcherProvider), VerifierRepository {

    private val validCertList = mutableListOf<String>()
    private val fetchStatus: MutableLiveData<Boolean> = MutableLiveData()
    private lateinit var context : Context

    override suspend fun syncData(applicationContext: Context): Boolean? {
        context= applicationContext
        Realm.init(applicationContext)

        return execute {
            fetchStatus.postValue(true)
            //insertValueToRealm()

            fetchValidationRules()
            getCRLStatus()

            if (fetchCertificates() == false) {
                fetchStatus.postValue(false)
                return@execute false
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
            if (recordCount.equals(0))
            {
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

    private suspend fun getCRLStatus() {
        val response = apiService.getCRLStatus(preferences.lastDownloadedVersion)
        val body = response.body() ?: run {
        }
        var crlstatus: CrlStatus = Gson().fromJson(response.body()?.string(), CrlStatus::class.java)
        Log.i("CRL Status", crlstatus.toString())

        //todo check if server crl version is newer than app crl version: done
        //then update
        //todo initialize lastDownloadedVersion
        if (outDatedVersion(crlstatus)) {
            if (noPendingDownload())
            {
                //todo add pending download in prefs
                //note: key: pendingDownload
                preferences.sizeSingleChunkInByte = crlstatus.sizeSingleChunkInByte
                preferences.numDiAdd = crlstatus.numDiAdd
                preferences.lastChunk = crlstatus.lastChunk //total number of chunks in a version
                preferences.numDiDelete = crlstatus.numDiDelete
                preferences.requestedVersion = crlstatus.version
                preferences.currentVersion = crlstatus.fromVersion
                preferences.authorizedToDownload = 0
                if (isFileOverThreshold(crlstatus) && preferences.authorizedToDownload ==0L) //todo: also consider if flag is set to 0 so it should be downloaded
                {
                    preferences.blockCRLdownload=1//todo note: probably not used, conisder removing it
                    //todo show popup in app using binding
                    //block autodownload and ask if user wants to download
                    //todo create a pref which states the app is blocked waiting for user confirm: done
                    preferences.authorizedToDownload = 0

                } else {//package size smaller than threshold
                    downloadChunk(crlstatus)
                    if (preferences.lastDownloadedChunk == crlstatus.lastChunk) {
                        //update current version
                        //we have processed the last chunk
                        preferences.currentVersion = preferences.requestedVersion
                        Log.i("chunk download", "Last chunk processed, versions updated")
                    }
                }
            }
            else if (preferences.authToResume == 1L)
            {
                //if pending YES
                //if NO same chunk size
                if (isSameChunkSize(crlstatus))
                {
                    //same version requested
                    if(sameRequestedVersion(crlstatus))
                    {
                        //At least one chunk downloaded.
                        if (atLeastOneChunkDownloaded(crlstatus))
                        {
                            //todo resume button;
                            Log.i("pending", "resume")
                            downloadChunk(crlstatus)
                        }
                        else
                        {
                            //todo start button
                            Log.i("pending", "start")
                            downloadChunk(crlstatus)
                        }

                    }
                    else
                    {
                        //NO same version requested
                        clearDB_clearPrefs()
                    }
                }
                else
                {
                    //if NO same chunk size
                    clearDB_clearPrefs()
                }

            } else {
                preferences.authToResume = 0L
            }
    }
        else //chunk size changed on server
        {
            clearDB_clearPrefs()
        }
    }

    private suspend fun getRevokeList(version: Long, chunk : Long = 1) {
        try{
            val response = apiService.getRevokeList(version, chunk) //destinationVersion, add chunk from prefs
            val body = response.body() ?: run {
            }
            var certificateRevocationList: CertificateRevocationList = Gson().fromJson(response.body()?.string(), CertificateRevocationList::class.java)
            //Log.i("CRL", certificateRevocationList.toString())
            if (version==certificateRevocationList.version) {
                processRevokeList(certificateRevocationList)
                preferences.lastDownloadedChunk = preferences.lastDownloadedChunk + 1
            }
            else
            {
                //todo dump realm and prefs
                clearDB_clearPrefs()
            }
        }
        catch (e: Exception)
        {
            Log.i("exception", e.localizedMessage.toString())
        }

    }

    private suspend fun processRevokeList(certificateRevocationList: CertificateRevocationList) {
        try{
            val revokedUcviList = certificateRevocationList.revokedUcvi

            if (revokedUcviList !=null)
            {
                //todo drop realmDB: probably not correct, should not be dropped
                //deleteAllfromRealm()

               /* for (revokedUcvi in revokedUcviList) {
                    realmRevokedPass.add(RevokedPass(revokedUcvi))
                }*/

                //todo process mRevokedUCVI adding them to realm (consider batch insert)
                //val realmInstance = RealmConnection.openRealm()
                Log.i("processRevokeList", " adding UCVI")
                insertListToRealm(revokedUcviList)
                //todo batch insert in realm
                /*for (revokedUcvi in revokedUcviList)
                {
                    //todo add to realm OR just do a batch insert in realm
                    Log.i("insert single ucvi", revokedUcvi.toString())
                }*/
            }
            else if (certificateRevocationList.delta!= null)
            {
                //Todo Delta check and processing
                Log.i("Delta", "delta")

                val deltaInsertList = certificateRevocationList.delta.insertions
                val deltaDeleteList = certificateRevocationList.delta.deletions

                if (deltaInsertList !=null)
                {
                    //Todo batch insert from Realm
                    Log.i("Delta", "delta insert")
                    insertListToRealm(deltaInsertList)
                }
                if(deltaDeleteList != null)
                {
                    //todo batch delete from Realm
                    Log.i("Delta", "delta delete")
                    deleteListFromRealm(deltaDeleteList)
                }

            }
        }
        catch (e: Exception)
        {
            Log.i("crl processing exception", e.localizedMessage.toString())
        }

    }

    private suspend fun clearDB_clearPrefs() {
        try {
            preferences.clear()
            deleteAllfromRealm()
            //todo add a restart sync: done, to test
            this.syncData(context)
        }
        catch (e : Exception)
        {
            Log.i("ClearDBClearPreds", e.localizedMessage)
        }
    }



    private suspend fun noPendingDownload(): Boolean {
        if (preferences.currentVersion == preferences.requestedVersion || preferences.authToResume == 1L)
            return true
        else {
            preferences.authToResume = 0L
            return false
        }
    }

    private suspend fun outDatedVersion(crlStatus: CrlStatus): Boolean {
        return (crlStatus.version != preferences.currentVersion)
    }

    private suspend fun sameRequestedVersion(crlStatus: CrlStatus): Boolean {
        return (crlStatus.version == preferences.requestedVersion)
    }

    private suspend fun isFileOverThreshold(crlStatus: CrlStatus): Boolean {
        return (crlStatus.totalSizeInByte > 5000000)
    }

    private suspend fun blockCRLdownload(crlStatus: CrlStatus): Boolean {
        return preferences.blockCRLdownload == 1L

    }

    private suspend fun chunkNotYetCompleted(crlStatus: CrlStatus): Boolean {
        return !noMoreChunks(crlStatus)
    }

    private suspend fun atLeastOneChunkDownloaded(crlStatus: CrlStatus): Boolean {
        // bigger than 0 should be ok as default value is set to 0
        return (preferences.lastDownloadedChunk >0)
    }

    private suspend fun isSameChunkSize(crlStatus: CrlStatus): Boolean {
        return (preferences.sizeSingleChunkInByte == crlStatus.sizeSingleChunkInByte)
    }

    private suspend fun downloadChunk(crlStatus: CrlStatus) {
        preferences.authorizedToDownload = 1 //related to big files
        preferences.authToResume= -1
        preferences.blockCRLdownload=0 //we are downloading, let's unblock any blocks
        while (preferences.lastDownloadedChunk < crlStatus.lastChunk) {
            getRevokeList(crlStatus.version, preferences.lastDownloadedChunk + 1)
        }
    }

    private suspend fun  noMoreChunks(crlStatus: CrlStatus): Boolean {
        var lastChunkDownloaded = preferences.currentChunk
        var allChunks = crlStatus.lastChunk
            return lastChunkDownloaded > allChunks
    }

    private suspend fun insertValueToRealm() {
        val realmName: String = "VerificaC19"
        val config = RealmConfiguration.Builder().name(realmName).build()
        val realm : Realm = Realm.getInstance(config)
        realm.executeTransaction { transactionRealm ->
            //transactionRealm.insertOrUpdate(array)
            var rp =  RevokedPass()
            rp.hashedUVCI = "test"
            transactionRealm.insert(rp)
        }
        Log.i("Revoke", "Inserted")
        /*val count = realm.where<RevokedPass>().findAll().size
        Log.i("Revoke", "Inserted $count")*/
        realm.close()
    }

    private suspend fun insertListToRealm(deltaInsertList: MutableList<String>) {
        try {
            val realmName: String = "VerificaC19"
            val config = RealmConfiguration.Builder().name(realmName).build()
            val realm: Realm = Realm.getInstance(config)

            var array = mutableListOf<RevokedPass>()

            for (deltaInsert in deltaInsertList) {
                var revokedPass: RevokedPass = RevokedPass()
                revokedPass.hashedUVCI = deltaInsert
                array.add(revokedPass)
            }
            try {
                realm.executeTransaction { transactionRealm ->
                    transactionRealm.insertOrUpdate(array)
                    //transactionRealm.insert(array)
                }
            }
            catch (e: RealmPrimaryKeyConstraintException)
            {
                Log.i("Revoke exc", e.localizedMessage)
            }
            Log.i("Revoke", "Inserted")
            val count = realm.where<RevokedPass>().findAll().size
            Log.i("Revoke", "Inserted $count")
            realm.close()
        }
        catch (e: Exception)
        {
            Log.i("Revoke exc2", e.localizedMessage)
        }
    }

    private suspend fun deleteAllfromRealm() {
        try {
            val realmName: String = "VerificaC19"
            val config = RealmConfiguration.Builder().name(realmName).build()
            val realm: Realm = Realm.getInstance(config)

            try {
                realm.executeTransaction { transactionRealm ->
                    transactionRealm.deleteAll()
                }
            }
            catch (e: RealmPrimaryKeyConstraintException)
            {
                Log.i("Revoke exc", e.localizedMessage)
            }
            realm.close()
        }
        catch (e: Exception)
        {
            Log.i("Revoke exc2", e.localizedMessage)
        }
    }

    private suspend fun deleteListFromRealm(deltaDeleteList: MutableList<String>) {
        try {
            val realmName: String = "VerificaC19"
            val config = RealmConfiguration.Builder().name(realmName).build()
            val realm: Realm = Realm.getInstance(config)

            try {
                realm.executeTransaction { transactionRealm ->
                //todo optimize by using a list delete
                    for (deltaDelete in deltaDeleteList) {
                        var query = realm.where(RevokedPass::class.java)
                        query.equalTo("hashedUVCI", deltaDelete)
                        var foundRevokedPass = query.findAll()
                        if (foundRevokedPass != null && foundRevokedPass.size > 0)
                        {
                            foundRevokedPass.deleteAllFromRealm()
                        }
                    }
                }
            }
            catch (e: RealmPrimaryKeyConstraintException)
            {
                Log.i("Revoke exc", e.localizedMessage)
            }
            val count = realm.where<RevokedPass>().findAll().size
            Log.i("Revoke", "deleted $count")
            realm.close()
        }
        catch (e: Exception)
        {
            Log.i("Revoke exc2", e.localizedMessage)
        }
    }

    companion object {

        const val HEADER_KID = "x-kid"
        const val HEADER_RESUME_TOKEN = "x-resume-token"
    }

}

