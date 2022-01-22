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
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.VerificationResult
import dgca.verifier.app.decoder.prefixvalidation.PrefixValidationService
import dgca.verifier.app.decoder.schema.SchemaValidator
import dgca.verifier.app.decoder.toBase64
import io.realm.Realm
import io.realm.RealmConfiguration
import it.ministerodellasalute.verificaC19sdk.BuildConfig
import it.ministerodellasalute.verificaC19sdk.VerificaDownloadInProgressException
import it.ministerodellasalute.verificaC19sdk.VerificaMinSDKVersionException
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepositoryImpl.Companion.REALM_NAME
import it.ministerodellasalute.verificaC19sdk.data.local.MedicinalProduct
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.data.local.RevokedPass
import it.ministerodellasalute.verificaC19sdk.data.local.ScanMode
import it.ministerodellasalute.verificaC19sdk.data.local.VerificaC19sdkRealmModule
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.util.Utility
import it.ministerodellasalute.verificaC19sdk.util.Utility.sha256
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import javax.inject.Inject
import java.security.cert.Certificate
import java.security.cert.X509Certificate

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

    fun getScanMode() = preferences.scanMode

    /**
     *
     * This method checks if the SDK version is obsoleted; if not, the [decode] method is called.
     *
     */
    @Throws(VerificaMinSDKVersionException::class, VerificaDownloadInProgressException::class)
    fun init(qrCodeText: String, fullModel: Boolean = false) {
        if (isSDKVersionObsoleted()) {
            throw VerificaMinSDKVersionException("l'SDK è obsoleto")
        } else {
            if (isDownloadInProgress()) {
                throw VerificaDownloadInProgressException("un download della DRL è in esecuzione")
            }
            decode(qrCodeText, fullModel, preferences.scanMode!!)
        }
    }

    private fun isDownloadInProgress(): Boolean {
        return preferences.currentChunk < preferences.totalChunk
    }

    @SuppressLint("SetTextI18n")
    fun decode(code: String, fullModel: Boolean, scanMode: String) {
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
                cryptoService.validate(cose, certificate as Certificate, verificationResult)
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

            val status = getCertificateStatus(certificateModel).applyFullModel(fullModel)
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

    private fun isRecoveryBis(
        recoveryStatements: List<RecoveryModel>?,
        cert: Certificate?
    ): Boolean {
        recoveryStatements?.first()?.takeIf { it.countryOfVaccination == Country.IT.value }
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
     *
     * This method gets the validation rules from the Shared Preferences as a JSON [String],
     * deserializing it in an [Array] of type [Rule].
     *
     */
    private fun getValidationRules(): Array<Rule> {
        val jsonString = preferences.validationRulesJson
        return Gson().fromJson(jsonString, Array<Rule>::class.java)
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

    fun getRecoveryCertStartDay(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getRecoveryCertPVStartDay(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    fun getRecoveryCertEndDay(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getRecoveryCertPvEndDay(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getMolecularTestStartHour(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.MOLECULAR_TEST_START_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    private fun getMolecularTestEndHour(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.MOLECULAR_TEST_END_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getRapidTestStartHour(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RAPID_TEST_START_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getRapidTestEndHour(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.RAPID_TEST_END_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineStartDayNotComplete(vaccineType: String): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineEndDayNotComplete(vaccineType: String): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineStartDayComplete(vaccineType: String): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineEndDayComplete(vaccineType: String): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    /**
     *
     * This method checks the given [CertificateModel] and returns the proper status as
     * [CertificateStatus].
     *
     */
    fun getCertificateStatus(cert: CertificateModel): CertificateStatus {
        if (cert.isRevoked) return CertificateStatus.REVOKED
        if (cert.certificateIdentifier.isEmpty()) return CertificateStatus.NOT_EU_DCC
        if (cert.isBlackListed) return CertificateStatus.NOT_VALID
        if (!cert.isValid) {
            return if (cert.isCborDecoded) CertificateStatus.NOT_VALID else
                CertificateStatus.NOT_EU_DCC
        }
        cert.recoveryStatements?.let {
            return checkRecoveryStatements(it, cert.certificate, cert.scanMode)
        }
        cert.tests?.let {
            if (cert.scanMode == ScanMode.BOOSTER || cert.scanMode == ScanMode.STRENGTHENED) return CertificateStatus.NOT_VALID
            return checkTests(it)
        }
        cert.vaccinations?.let {
            return checkVaccinations(it, cert.scanMode)
        }
        cert.exemptions?.let {
            return checkExemptions(it, cert.scanMode)
        }

        return CertificateStatus.NOT_VALID
    }

    /**
     * This method checks the [Exemption] and returns a proper [CertificateStatus]
     * after checking the validity start and end dates.
     */
    private fun checkExemptions(
        it: List<Exemption>,
        scanMode: String
    ): CertificateStatus {

        try {
            val startDate: LocalDate = LocalDate.parse(clearExtraTime(it.last().certificateValidFrom))
            val endDate: LocalDate? = it.last().certificateValidUntil?.let {
                LocalDate.parse(clearExtraTime(it))
            }
            Log.d("dates", "start:$startDate end: $endDate")

            if (startDate.isAfter(LocalDate.now())) {
                return CertificateStatus.NOT_VALID_YET
            }
            endDate?.let {
                if (LocalDate.now().isAfter(endDate)) {
                    return CertificateStatus.NOT_VALID
                }
            }
            return if (scanMode == ScanMode.BOOSTER) {
                CertificateStatus.TEST_NEEDED
            } else CertificateStatus.VALID
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
    }

    /**
     *
     * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
     * the proper status as [CertificateStatus].
     *
     */
    private fun checkVaccinations(
        it: List<VaccinationModel>?,
        scanMode: String
    ): CertificateStatus {

        // Check if vaccine is present in setting list; otherwise, return not valid
        val vaccineEndDayComplete = getVaccineEndDayComplete(it!!.last().medicinalProduct)
        val isValid = vaccineEndDayComplete.isNotEmpty()
        if (!isValid) return CertificateStatus.NOT_VALID
        val isSputnikNotFromSanMarino =
            it.last().medicinalProduct == "Sputnik-V" && it.last().countryOfVaccination != "SM"
        if (isSputnikNotFromSanMarino) return CertificateStatus.NOT_VALID

        try {
            when {
                it.last().doseNumber < it.last().totalSeriesOfDoses -> {
                    val startDate: LocalDate =
                        LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineStartDayNotComplete(it.last().medicinalProduct))
                                    .toLong()
                            )

                    val endDate: LocalDate =
                        LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineEndDayNotComplete(it.last().medicinalProduct))
                                    .toLong()
                            )
                    Log.d("dates", "start:$startDate end: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now()
                            .isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> if (ScanMode.BOOSTER == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
                    }
                }
                it.last().doseNumber >= it.last().totalSeriesOfDoses -> {
                    val startDate: LocalDate
                    val endDate: LocalDate
                    if (it.last().medicinalProduct == MedicinalProduct.JOHNSON && ((it.last().doseNumber > it.last().totalSeriesOfDoses) ||
                                (it.last().doseNumber == it.last().totalSeriesOfDoses && it.last().doseNumber >= 2))
                    ) {
                        startDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))

                        endDate = LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineEndDayComplete(it.last().medicinalProduct))
                                    .toLong()
                            )
                    } else {
                        startDate =
                            LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                                .plusDays(
                                    Integer.parseInt(getVaccineStartDayComplete(it.last().medicinalProduct))
                                        .toLong()
                                )

                        endDate =
                            LocalDate.parse(clearExtraTime(it.last().dateOfVaccination))
                                .plusDays(
                                    Integer.parseInt(getVaccineEndDayComplete(it.last().medicinalProduct))
                                        .toLong()
                                )
                    }
                    Log.d("dates", "start:$startDate end: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now()
                            .isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> {
                            when (scanMode) {
                                ScanMode.BOOSTER -> {
                                    if (it.last().medicinalProduct == MedicinalProduct.JOHNSON) {
                                        if (it.last().doseNumber == it.last().totalSeriesOfDoses && it.last().doseNumber < 2) return CertificateStatus.TEST_NEEDED
                                    } else {
                                        if ((it.last().doseNumber == it.last().totalSeriesOfDoses && it.last().doseNumber < 3))
                                            return CertificateStatus.TEST_NEEDED
                                    }
                                    return CertificateStatus.VALID
                                }
                                else -> return CertificateStatus.VALID
                            }
                        }
                    }
                }
                else -> CertificateStatus.NOT_VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
        return CertificateStatus.NOT_EU_DCC
    }

    /**
     *
     * This method checks the given tests passed as a [List] of [TestModel] and returns the proper
     * status as [CertificateStatus].
     *
     */
    private fun checkTests(it: List<TestModel>?): CertificateStatus {
        if (it!!.last().resultType == TestResult.DETECTED) {
            return CertificateStatus.NOT_VALID
        }
        try {
            val odtDateTimeOfCollection = OffsetDateTime.parse(it.last().dateTimeOfCollection)
            val ldtDateTimeOfCollection = odtDateTimeOfCollection.toLocalDateTime()

            val testType = it!!.last().typeOfTest

            val startDate: LocalDateTime
            val endDate: LocalDateTime

            when (testType) {
                TestType.MOLECULAR.value -> {
                    startDate = ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(getMolecularTestStartHour()).toLong())
                    endDate = ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(getMolecularTestEndHour()).toLong())
                }
                TestType.RAPID.value -> {
                    startDate = ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(getRapidTestStartHour()).toLong())
                    endDate = ldtDateTimeOfCollection
                        .plusHours(Integer.parseInt(getRapidTestEndHour()).toLong())
                }
                else -> {
                    return CertificateStatus.NOT_VALID
                }
            }

            Log.d("dates", "start:$startDate end: $endDate")
            return when {
                startDate.isAfter(LocalDateTime.now()) -> CertificateStatus.NOT_VALID_YET
                LocalDateTime.now()
                    .isAfter(endDate) -> CertificateStatus.NOT_VALID
                else -> CertificateStatus.VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
    }

    /**
     *
     * This method checks the given recovery statements passed as a [List] of [RecoveryModel] and
     * returns the proper status as [CertificateStatus].
     *
     */
    private fun checkRecoveryStatements(
        it: List<RecoveryModel>,
        certificate: Certificate?,
        scanMode: String
    ): CertificateStatus {
        val isRecoveryBis = isRecoveryBis(
            it,
            certificate
        )
        val recoveryCertEndDay =
            if (isRecoveryBis
            ) getRecoveryCertPvEndDay() else getRecoveryCertEndDay()
        val recoveryCertStartDay =
            if (isRecoveryBis) getRecoveryCertPVStartDay() else getRecoveryCertStartDay()
        try {
            val startDate: LocalDate =
                LocalDate.parse(clearExtraTime(it.last().certificateValidFrom))

            val endDate: LocalDate =
                LocalDate.parse(clearExtraTime(it.last().certificateValidUntil))

            Log.d("dates", "start:$startDate end: $endDate")
            return when {
                startDate.plusDays(
                    Integer.parseInt(recoveryCertStartDay)
                        .toLong()
                ).isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                LocalDate.now()
                    .isAfter(
                        startDate.plusDays(
                            Integer.parseInt(recoveryCertEndDay)
                                .toLong()
                        )
                    ) -> CertificateStatus.NOT_VALID
                else -> return if (scanMode == ScanMode.BOOSTER) CertificateStatus.TEST_NEEDED else CertificateStatus.VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_VALID
        }
    }

    private fun clearExtraTime(strDateTime: String): String {
        try {
            if (strDateTime.contains("T")) {
                return strDateTime.substring(0, strDateTime.indexOf("T"))
            }
            return strDateTime
        } catch (e: Exception) {
            return strDateTime
        }
    }

    fun getAppMinVersion(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value }
            ?.let {
                it.value
            } ?: run {
            ""
        }
    }

    /**
     *
     * This method invokes the [getValidationRules] method to obtain the validation rules and then
     * extract from it the part regarding the minimum SDK version.
     *
     */
    private fun getSDKMinVersion(): String {
        return getValidationRules().find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value }
            ?.let {
                it.value
            } ?: run {
            ""
        }
    }

    /**
     *
     * This method invokes the [getSDKMinVersion] method to obtain the minimum SDK version and then
     * compare it with the current SDK version in use.
     *
     */
    private fun isSDKVersionObsoleted(): Boolean {
        this.getSDKMinVersion().let {
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
        if (hash != "") {
            val config = RealmConfiguration.Builder()
                .name(REALM_NAME)
                .modules(VerificaC19sdkRealmModule())
                .allowQueriesOnUiThread(true)
                .build()
            val realm: Realm = Realm.getInstance(config)
            Log.i("Revoke", "Searching")
            val query = realm.where(RevokedPass::class.java)
            query.equalTo("hashedUVCI", hash)
            val foundRevokedPass = query.findAll()
            return if (foundRevokedPass != null && foundRevokedPass.size > 0) {
                Log.i("Revoke", "Found!")
                true
            } else
                false
        } else {
            return true
        }
    }

}
