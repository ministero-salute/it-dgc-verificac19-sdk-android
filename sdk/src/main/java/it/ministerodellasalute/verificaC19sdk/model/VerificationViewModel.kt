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
import it.ministerodellasalute.verificaC19sdk.*
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.local.realm.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.model.validation.Validator
import it.ministerodellasalute.verificaC19sdk.util.*
import it.ministerodellasalute.verificaC19sdk.util.Utility.sha256
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.cert.Certificate
import java.security.cert.X509Certificate
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

    fun getScanMode() = ScanMode.from(preferences.scanMode!!)

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
        return preferences.currentChunk < preferences.totalChunk
    }

    @SuppressLint("SetTextI18n")
    fun decode(code: String, fullModel: Boolean, scanMode: ScanMode) {
        viewModelScope.launch {
            _inProgress.value = true
            var greenCertificate: GreenCertificate? = null
            val verificationResult = VerificationResult()

            var certificateIdentifier = ""
            var blackListCheckResult = false
            var certificate: Certificate? = null
            var exemptions: Array<Exemption>? = null

            withContext(dispatcherProvider.getIO()) {
                val plainInput = prefixValidationService.decode(code, verificationResult)
                val compressedCose = base45Service.decode(plainInput, verificationResult)
                val cose: ByteArray? = compressorService.decode(compressedCose, verificationResult)
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
                isRevoked = isCertificateRevoked(certificateIdentifier.sha256())
                this.scanMode = scanMode
                this.certificateIdentifier = certificateIdentifier
                this.certificate = certificate
                this.exemptions = exemptions?.toList()
            }
            val ruleSet = RuleSet(preferences.validationRulesJson)
            val status = getCertificateStatus(certificateModel, ruleSet).applyFullModel(fullModel)
            _certificate.value = certificateModel.toCertificateViewBean(status)
        }
    }

    private fun isRecoveryBis(
        recoveryStatements: List<RecoveryModel>?,
        cert: Certificate?
    ): Boolean {
        recoveryStatements?.first()?.takeIf { it.country == Country.IT.value }
            .let {
                cert?.let {
                    (cert as X509Certificate).extendedKeyUsage?.find { keyUsage -> CertCode.OID_RECOVERY.value == keyUsage || CertCode.OID_ALT_RECOVERY.value == keyUsage }
                        ?.let {
                            return true
                        }
                }
            } ?: return false
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
            greenCertificate?.vaccinations?.get(0)?.certificateIdentifier != null -> {
                greenCertificate.vaccinations?.get(0)?.certificateIdentifier!!

            }
            greenCertificate?.tests?.get(0)?.certificateIdentifier != null -> {
                greenCertificate.tests?.get(0)?.certificateIdentifier!!
            }
            greenCertificate?.recoveryStatements?.get(0)?.certificateIdentifier != null -> {
                greenCertificate.recoveryStatements?.get(0)?.certificateIdentifier!!
            }
            else -> ""
        }
    }

    /**
     *
     * This method checks the given [CertificateModel] and returns the proper status as
     * [CertificateStatus].
     *
     */
    fun getCertificateStatus(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        return Validator.validate(certificateModel, ruleSet)
        }


    /**
     *
     * This method invokes the [getSDKMinVersion] method to obtain the minimum SDK version and then
     * compare it with the current SDK version in use.
     *
     */
    private fun isSDKVersionObsolete(): Boolean {
        val ruleSet = RuleSet(preferences.validationRulesJson)
        ruleSet.getSDKMinVersion().let {
            if (Utility.versionCompare(it, BuildConfig.SDK_VERSION) > 0) {
                return true
            }
        }
        return false
    }


    private fun isCertificateRevoked(hash: String): Boolean {
        if (!preferences.isDrlSyncActive) {
            return false
        }
        return if (hash.isNotEmpty()) {
            val realm: Realm = Realm.getDefaultInstance()
            Log.i("Revoke", "Searching")
            val query = realm.where(RevokedPass::class.java)
            query.equalTo("hashedUVCI", hash)
            val foundRevokedPass = query.findAll()
            val passRevokedFound = foundRevokedPass.size
            realm.close()
            if (foundRevokedPass != null && passRevokedFound > 0) {
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

}
