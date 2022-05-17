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
 *  Created by danielsp on 9/15/21, 2:32 PM
 */

package it.ministerodellasalute.verificaC19sdk.model

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dgca.verifier.app.decoder.base45.Base45Service
import dgca.verifier.app.decoder.cbor.CborService
import dgca.verifier.app.decoder.cbor.GreenCertificateData
import dgca.verifier.app.decoder.compression.CompressorService
import dgca.verifier.app.decoder.cose.CoseService
import dgca.verifier.app.decoder.cose.CryptoService
import dgca.verifier.app.decoder.model.CertificateType
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.VerificationResult
import dgca.verifier.app.decoder.prefixvalidation.PrefixValidationService
import dgca.verifier.app.decoder.schema.SchemaValidator
import dgca.verifier.app.decoder.toBase64
import io.realm.Realm
import io.realm.RealmResults
import it.ministerodellasalute.verificaC19sdk.*
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.local.realm.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.local.realm.RevokedPassEU
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.model.validation.Settings
import it.ministerodellasalute.verificaC19sdk.model.validation.Validator
import it.ministerodellasalute.verificaC19sdk.util.*
import it.ministerodellasalute.verificaC19sdk.util.Utility.getDccSignatureSha256
import it.ministerodellasalute.verificaC19sdk.util.Utility.sha256
import it.ministerodellasalute.verificaC19sdk.util.Utility.toSha256HexString
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.cert.Certificate
import java.util.*
import javax.inject.Inject

private const val TAG = "VerificationViewModel"

/**
 *
 * This class contains all the methods regarding the verification of the certifications.
 *
 */
@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val prefixValidationService: PrefixValidationService,
    private val base45Service: Base45Service,
    private val compressorService: CompressorService,
    private val cryptoService: CryptoService,
    private val coseService: CoseService,
    private val schemaValidator: SchemaValidator,
    private val cborService: CborService,
    private val verifierRepository: VerifierRepository,
    private val preferences: Preferences,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _certificate = MutableLiveData<CertificateViewBean?>()
    val certificate: LiveData<CertificateViewBean?> = _certificate

    private val _inProgress = MutableLiveData<Boolean>()
    val inProgress: LiveData<Boolean> = _inProgress

    /**
     *
     * This method gets the current status of the camera stored in the Shared Preferences.
     *
     */
    fun getFrontCameraStatus() = preferences.isFrontCameraActive

    /**
     *
     * This method sets the current status of the camera stored in the Shared Preferences.
     *
     */
    fun setFrontCameraStatus(value: Boolean) =
        run { preferences.isFrontCameraActive = value }

    /**
     *
     * This method gets the current status of the totem mode stored in the Shared Preferences.
     *
     */
    fun getTotemMode() = preferences.isTotemModeActive

    /**
     *
     * This method sets the current status of the totem mode stored in the Shared Preferences.
     *
     */
    fun setTotemMode(value: Boolean) =
        run { preferences.isTotemModeActive = value }

    fun getScanMode() = ScanMode.from(preferences.scanMode)

    fun getDoubleScanFlag() = preferences.isDoubleScanFlow

    fun setDoubleScanFlag(flag: Boolean) = run { preferences.isDoubleScanFlow = flag }

    fun getUserName() = preferences.userName

    fun setUserName(firstName: String) = run { preferences.userName = firstName }

    fun getRuleSet() = Settings(preferences.validationRulesJson)

    private fun isDrlSyncActive() = preferences.isDrlSyncActive || preferences.isDrlSyncActiveEU

    /**
     *
     * This method checks if the SDK version is obsoleted; if not, the [decode] method is called.
     *
     */
    @Throws(VerificaMinSDKVersionException::class, VerificaDownloadInProgressException::class)
    fun init(qrCodeText: String, fullModel: Boolean = false) {
        if (isSDKVersionObsolete()) {
            throw VerificaMinSDKVersionException("l'SDK è obsoleto")
        } else {
            if (isDownloadInProgress()) {
                throw VerificaDownloadInProgressException("un download della DRL è in esecuzione")
            }
            decode(qrCodeText, fullModel, ScanMode.from(preferences.scanMode))
        }
    }

    private fun isDownloadInProgress(): Boolean {
        val downloadActiveIT = preferences.drlStateIT.currentChunk < preferences.drlStateIT.totalChunk
        val downloadActiveEU = preferences.drlStateEU.currentChunk < preferences.drlStateEU.totalChunk

        return downloadActiveIT || downloadActiveEU
    }


    @SuppressLint("SetTextI18n")
    fun decode(code: String, fullModel: Boolean, scanMode: ScanMode?) {
        viewModelScope.launch {
            _inProgress.value = true
            var greenCertificate: GreenCertificate? = null
            val verificationResult = VerificationResult()

            var certificateIdentifier = ""
            var certificateCountry = ""
            var blackListCheckResult = false
            var certificate: Certificate? = null
            var exemptions: Array<Exemption>? = null
            val cose: ByteArray?

            withContext(dispatcherProvider.getIO()) {
                val plainInput = prefixValidationService.decode(code, verificationResult)
                val compressedCose = base45Service.decode(plainInput, verificationResult)
                cose = compressorService.decode(compressedCose, verificationResult)
                if (cose == null) {
                    Log.d(TAG, "Verification failed: Too many bytes read")
                    return@withContext
                }

                val coseData = coseService.decode(cose, verificationResult)
                if (coseData == null) {
                    Log.d(TAG, "Verification failed: COSE not decoded")
                    return@withContext
                }

                val kid = coseData.kid
                if (kid == null) {
                    Log.d(TAG, "Verification failed: cannot extract kid from COSE")
                    return@withContext

                }

                schemaValidator.validate(coseData.cbor, verificationResult)

                val decodeData = cborService.decodeData(coseData.cbor, verificationResult)
                exemptions = extractExemption(decodeData)
                greenCertificate = decodeData?.greenCertificate

                certificate = verifierRepository.getCertificate(kid.toBase64())
                certificateIdentifier = extractUVCI(greenCertificate, exemptions?.first())
                certificateCountry = extractCountry(greenCertificate, exemptions?.first())

                if (certificate == null) {
                    Log.d(TAG, "Verification failed: failed to load certificate")
                    return@withContext
                }
                cryptoService.validate(cose, certificate as Certificate, verificationResult, greenCertificate?.getType() ?: CertificateType.UNKNOWN)
                blackListCheckResult = verifierRepository.checkInBlackList(certificateIdentifier)
            }

            _inProgress.value = false
            val certificateModel = greenCertificate.toCertificateModel(verificationResult).apply {
                isBlackListed = blackListCheckResult
                isRevoked = isCertificateRevoked(certificateIdentifier, certificateCountry, cose)
                tests?.let {
                    it.last().isPreviousScanModeBooster = scanMode == ScanMode.BOOSTER
                }
                this.scanMode = if (getDoubleScanFlag()) ScanMode.DOUBLE_SCAN else scanMode
                this.certificateIdentifier = certificateIdentifier
                this.certificate = certificate
                this.exemptions = exemptions?.toList()
            }
            val settings = Settings(preferences.validationRulesJson)
            val status = getCertificateStatus(certificateModel, settings).applyFullModel(fullModel)
            _certificate.value = certificateModel.toCertificateViewBean(status)
        }
    }

    /**
     * This method takes care of retrieving the [Exemption] from the json received
     * by the Decoder library, and then returns it if present, otherwise it returns null.
     */
    private fun extractExemption(
        decodeData: GreenCertificateData?
    ): Array<Exemption>? {
        val jsonObject = JSONObject(decodeData!!.hcertJson)
        val exemptionJson = if (jsonObject.has("e")) jsonObject.getString("e") else null

        exemptionJson?.let {
            Log.i("exemption found", it)
            return Gson().fromJson(exemptionJson, Array<Exemption>::class.java)
        }
        return null
    }

    /**
     * This method extracts the UCVI from an Exemption, Vaccine, Recovery or Test
     * based on what was received.
     */
    private fun extractUVCI(greenCertificate: GreenCertificate?, exemption: Exemption?): String {
        return when {
            exemption != null -> {
                exemption.certificateIdentifier
            }
            else -> greenCertificate?.getDgci()!!
        }
    }

    private fun extractCountry(greenCertificate: GreenCertificate?, exemption: Exemption?): String {
        val country = when {
            exemption != null -> {
                exemption.countryOfVaccination
            }
            else -> greenCertificate?.getIssuingCountry()!!
        }
        return country.uppercase(Locale.getDefault())
    }

    /**
     *
     * This method checks the given [CertificateModel] and returns the proper status as
     * [CertificateStatus].
     *
     */
    fun getCertificateStatus(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        return Validator.validate(certificateModel, settings)
    }

    /**
     *
     * This method invokes the [getSDKMinVersion] method to obtain the minimum SDK version and then
     * compare it with the current SDK version in use.
     *
     */
    private fun isSDKVersionObsolete(): Boolean {
        val settings = Settings(preferences.validationRulesJson)
        settings.getSDKMinVersion().let {
            if (Utility.versionCompare(it, BuildConfig.SDK_VERSION) > 0) {
                return true
            }
        }
        return false
    }

    private fun isCertificateRevoked(ucvi: String, certificateCountry: String, cose: ByteArray?): Boolean {
        if (!isDrlSyncActive()) return false

        return if (ucvi.isNotEmpty()) {
            val realm: Realm = Realm.getDefaultInstance()
            var foundRevokedPassIT: RealmResults<RevokedPass>? = null
            var foundRevokedPassEU: RealmResults<RevokedPassEU>? = null
            var revokedCountIT = 0
            var revokedCountEU = 0

            when {
                certificateCountry == Country.IT.value && preferences.isDrlSyncActive -> {
                    val queryIT = realm.where(RevokedPass::class.java)
                    queryIT.equalTo("hashedUVCI", ucvi.sha256())
                    foundRevokedPassIT = queryIT.findAll()
                    revokedCountIT = foundRevokedPassIT.size
                }
                certificateCountry != Country.IT.value && preferences.isDrlSyncActiveEU -> {
                    val listOfHash = cose?.let { extractHash(ucvi, certificateCountry, it) }

                    run findRevokedPassEU@ {
                        listOfHash?.forEach {
                            val queryEU = realm.where(RevokedPassEU::class.java)
                            queryEU.equalTo("hashedUVCI", it)
                            foundRevokedPassEU = queryEU.findAll()
                            revokedCountEU = foundRevokedPassEU?.size!!
                            if (revokedCountEU > 0) return@findRevokedPassEU
                        }
                    }
                }
            }

            realm.close()
            if ((foundRevokedPassIT != null && revokedCountIT > 0) || (foundRevokedPassEU != null && revokedCountEU > 0)) {
                Log.i("Revoke", "Found!")
                true
            } else {
                Log.i("Revoke", "Not Found!")
                false
            }
        } else {
            true
        }
    }

    private fun extractHash(certificateIdentifier: String, country: String, cose: ByteArray): MutableList<String> {
        return mutableListOf(
            certificateIdentifier.toByteArray().toSha256HexString(),
            (country + certificateIdentifier).toByteArray().toSha256HexString(),
            cose.getDccSignatureSha256()
        )
    }
}
