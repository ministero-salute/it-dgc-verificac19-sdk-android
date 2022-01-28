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
 *  Created by nicolamcornelio on 1/26/22, 2:06 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.local.realm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import it.ministerodellasalute.verificaC19sdk.data.local.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.local.VerificaC19sdkRealmModule
import it.ministerodellasalute.verificaC19sdk.data.remote.model.CertificateRevocationList
import it.ministerodellasalute.verificaC19sdk.model.CertificateModel
import it.ministerodellasalute.verificaC19sdk.utility.AndroidTestUtility
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class RealmTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    private lateinit var realm: Realm
    private lateinit var config: RealmConfiguration

    @Before
    fun setUp() {
        config = RealmConfiguration.Builder()
            .name("RevocationTest")
            .modules(VerificaC19sdkRealmModule())
            .inMemory()
            .allowQueriesOnUiThread(true)
            .build()
        realm = Realm.getInstance(config)
    }

    @After
    fun tearDown() {
        realm.close()
    }

    @Test
    fun isCertificateRevoked_givenGreenCertificate_revokedCertificate() {
        val inputStream = AndroidTestUtility.openAndroidTestAssetsFile("certificate_model_recovery_valid.json")
        val greenCertificate = AndroidTestUtility.readInputStream(inputStream)

        val model = Gson().fromJson(
            greenCertificate, CertificateModel::class.java
        )
        val certificateIdentifier = (model.recoveryStatements?.get(0)?.certificateIdentifier)
        val realmModel = certificateIdentifier?.let { RevokedPass(it) }
        realm.executeTransaction { transactionRealm ->
            transactionRealm.insertOrUpdate(realmModel)
        }

        val query = realm.where(RevokedPass::class.java)
        query.equalTo("hashedUVCI", "01IT041850C8D71244B4A2E12721E043B9F9#8")
        val foundRevokedPass = query.findAll()
        assertThat(foundRevokedPass.size).isGreaterThan(0)
    }

    @Test
    fun snapShotInsertion_givenChunk_insertionSuccessfully() {
        val inputStream = AndroidTestUtility.openAndroidTestAssetsFile("drl_chunk_of_snapshot.json")
        val snapshot = AndroidTestUtility.readInputStream(inputStream)

        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            snapshot,
            CertificateRevocationList::class.java
        )
        val revokedUcvis = certificateRevocationList.revokedUcvi
        val listToAdd = revokedUcvis.map { RevokedPass(it) }

        realm.executeTransaction { transactionRealm ->
            transactionRealm.insertOrUpdate(listToAdd)
        }
        val query = realm.where(RevokedPass::class.java)
        val revokedCertificates = query.findAll()
        assertThat(revokedCertificates.size).isEqualTo(10)
    }

    @Test
    fun deltaInsertion_givenChunk_insertionSuccessfully() {
        val inputStream = AndroidTestUtility.openAndroidTestAssetsFile("drl_chunk_of_delta_insertion.json")
        val snapshot = AndroidTestUtility.readInputStream(inputStream)

        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            snapshot,
            CertificateRevocationList::class.java
        )
        val revokedUcvis = certificateRevocationList.delta.insertions
        val listToAdd = revokedUcvis.map { RevokedPass(it) }

        realm.executeTransaction { transactionRealm ->
            transactionRealm.insertOrUpdate(listToAdd)
        }
        val query = realm.where(RevokedPass::class.java)
        val revokedCertificates = query.findAll()
        assertThat(revokedCertificates.size).isEqualTo(7)
    }

    @Test
    fun isCertificateRevokedAfterDeletion_givenGreenCertificate_notRevokedCertificate() {
        var inputStream = AndroidTestUtility.openAndroidTestAssetsFile("certificate_model_vaccination_valid.json")
        val greenCertificate = AndroidTestUtility.readInputStream(inputStream)
        val model = Gson().fromJson(
            greenCertificate, CertificateModel::class.java
        )
        val certificateIdentifier = (model.vaccinations?.get(0)?.certificateIdentifier)
        val realmModel = certificateIdentifier?.let { RevokedPass(it) }
        realm.executeTransaction { transactionRealm ->
            transactionRealm.insertOrUpdate(realmModel)
        }
        var query = realm.where(RevokedPass::class.java)
        query.equalTo("hashedUVCI", "01ITCA73992479C04B28925FBF3ACA9A4AB8#4")
        var foundRevokedPass = query.findAll()
        assertThat(foundRevokedPass.size).isEqualTo(1)

        inputStream = AndroidTestUtility.openAndroidTestAssetsFile("drl_chunk_of_delta_deletion.json")
        val delta = AndroidTestUtility.readInputStream(inputStream)
        val certificateRevocationList: CertificateRevocationList = Gson().fromJson(
            delta,
            CertificateRevocationList::class.java
        )
        val revokedUcvis = certificateRevocationList.delta.insertions
        val listToAdd = revokedUcvis.map { RevokedPass(it) }
        realm.executeTransaction { transactionRealm ->
            transactionRealm.insertOrUpdate(listToAdd)
        }

        val unrevokedUcvis = certificateRevocationList.delta.deletions
        realm.executeTransaction { transactionRealm ->
            val revokedPassesToDelete = transactionRealm.where<RevokedPass>().`in`("hashedUVCI", unrevokedUcvis.toTypedArray()).findAll()
            revokedPassesToDelete.deleteAllFromRealm()
        }
        query = realm.where(RevokedPass::class.java)
        query.equalTo("hashedUVCI", "01ITCA73992479C04B28925FBF3ACA9A4AB8#4")
        foundRevokedPass = query.findAll()
        assertThat(foundRevokedPass.size).isEqualTo(0)
    }
}