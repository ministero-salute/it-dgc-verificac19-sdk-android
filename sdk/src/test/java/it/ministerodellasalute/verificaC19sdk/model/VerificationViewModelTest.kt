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
 *  Created by simonepirozzi on 20/10/21, 11:25
 */

package it.ministerodellasalute.verificaC19sdk.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import dgca.verifier.app.decoder.base45.Base45Service
import dgca.verifier.app.decoder.cbor.CborService
import dgca.verifier.app.decoder.compression.CompressorService
import dgca.verifier.app.decoder.cose.CoseService
import dgca.verifier.app.decoder.cose.CryptoService
import dgca.verifier.app.decoder.prefixvalidation.PrefixValidationService
import dgca.verifier.app.decoder.schema.SchemaValidator
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.data.local.Preferences
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.utils.Base64
import it.ministerodellasalute.verificaC19sdk.utils.MainCoroutineScopeRule
import it.ministerodellasalute.verificaC19sdk.utils.mock.MockDataUtils
import it.ministerodellasalute.verificaC19sdk.utils.mock.ServiceMocks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.*


class VerificationViewModelTest {

    companion object {
        private const val CERTIFICATE_MODEL_RECOVERY_VALID = "certificate_model_recovery_valid.json"
        private const val CERTIFICATE_MODEL_RECOVERY_NOT_VALID_YET = "certificate_model_recovery_not_valid_yet.json"
        private const val CERTIFICATE_MODEL_RECOVERY_NOT_VALID = "certificate_model_recovery_not_valid.json"

        private const val CERTIFICATE_MODEL_VACCINATION_VALID = "certificate_model_vaccination_valid.json"
        private const val CERTIFICATE_MODEL_VACCINATION_NOT_VALID_YET = "certificate_model_vaccination_not_valid_yet.json"
        private const val CERTIFICATE_MODEL_VACCINATION_NOT_VALID = "certificate_model_vaccination_not_valid.json"
        private const val CERTIFICATE_MODEL_VACCINATION_VALID_NOT_IT = "certificate_model_vaccination_valid_NOT_IT.json"

        private const val CERTIFICATE_MODEL_TEST_VALID = "certificate_model_test_valid.json"
        private const val CERTIFICATE_MODEL_TEST_NOT_VALID_YET = "certificate_model_test_not_valid_yet.json"
        private const val CERTIFICATE_MODEL_TEST_NOT_VALID = "certificate_model_test_not_valid.json"
    }

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    val mainCoroutineScopeRule: MainCoroutineScopeRule = MainCoroutineScopeRule()

    @RelaxedMockK
    private lateinit var prefixValidationService: PrefixValidationService

    @RelaxedMockK
    private lateinit var base45Service: Base45Service

    @RelaxedMockK
    private lateinit var compressorService: CompressorService

    @RelaxedMockK
    private lateinit var cryptoService: CryptoService

    @RelaxedMockK
    private lateinit var coseService: CoseService

    @RelaxedMockK
    private lateinit var schemaValidator: SchemaValidator

    @RelaxedMockK
    private lateinit var cborService: CborService

    @RelaxedMockK
    private lateinit var verifierRepository: VerifierRepository

    @RelaxedMockK
    private lateinit var preferences: Preferences

    private lateinit var viewModel: VerificationViewModel

    @RelaxedMockK
    private val dispatcherProvider: DispatcherProvider = mockk()

    private val nowLocalDate = LocalDate.of(2021, 10, 19)
    private val nowLocalDateTime = LocalDateTime.of(2021, Month.OCTOBER, 19, 14, 57, 54, 0)


    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { dispatcherProvider.getIO() }.returns(mainCoroutineScopeRule.testDispatcher)

        viewModel = VerificationViewModel(
            prefixValidationService, base45Service, compressorService,
            cryptoService, coseService, schemaValidator, cborService, verifierRepository, preferences, dispatcherProvider
        )
    }

    @Before
    fun `Bypass android_util_Base64 to java_util_Base64`() {
        mockkStatic(android.util.Base64::class)
        val arraySlot = slot<ByteArray>()
        every {
            android.util.Base64.encodeToString(capture(arraySlot), android.util.Base64.NO_WRAP)
        } answers {
            java.util.Base64.getEncoder().encodeToString(arraySlot.captured)
        }

        val stringSlot = slot<String>()
        every {
            android.util.Base64.decode(capture(stringSlot), android.util.Base64.NO_WRAP)
        } answers {
            java.util.Base64.getDecoder().decode(stringSlot.captured)
        }
    }

    @Before
    fun `fix the local date =)`() {
        mockkStatic(LocalDate::class)
    }

    @Before
    fun `fix the local date time =)`() {
        mockkStatic(LocalDateTime::class)
    }

    @Test
    fun getCertificateStatusRecovery() {

        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)
        every { LocalDate.now() } returns nowLocalDate

        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID)
    }

    @Test
    fun getCertificateStatusVaccination() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)
        every { LocalDate.now() } returns nowLocalDate
        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID)
    }

    @Test
    fun getCertificateStatusTest() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)
        every { LocalDateTime.now() } returns nowLocalDateTime

        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model)
        assertEquals(result, CertificateStatus.NOT_VALID)

    }

    @Test
    fun getRecoveryCertStartDay() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getRecoveryCertStartDay()

        assertEquals(expectedData, "0")
    }

    @Test
    fun getRecoveryCertEndDay() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getRecoveryCertEndDay()

        assertEquals(expectedData, "180")
    }

    @Test
    fun getRapidTestStartHour() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getRapidTestStartHour()

        assertEquals(expectedData, "0")
    }

    @Test
    fun getRapidTestEndHour() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getRapidTestEndHour()

        assertEquals(expectedData, "48")
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-20-1528 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayNotComplete("EU/1/20/1528")

        assertEquals(expectedData, "15")
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-20-1528 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayNotComplete("EU/1/20/1528")

        assertEquals(expectedData, "42")
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-20-1528 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayComplete("EU/1/20/1528")

        assertEquals(expectedData, "0")
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-20-1528 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayComplete("EU/1/20/1528")

        assertEquals(expectedData, "365")
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-20-1507 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayNotComplete("EU/1/20/1507")

        assertEquals(expectedData, "15")
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-20-1507 vaccin type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayNotComplete("EU/1/20/1507")

        assertEquals(expectedData, "42")
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-20-1507 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayComplete("EU/1/20/1507")

        assertEquals(expectedData, "0")
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-20-1507 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayComplete("EU/1/20/1507")

        assertEquals(expectedData, "365")
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-21-1529 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayNotComplete("EU/1/21/1529")

        assertEquals(expectedData, "15")
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-21-1529 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayNotComplete("EU/1/21/1529")

        assertEquals(expectedData, "84")
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-21-1529 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayComplete("EU/1/21/1529")

        assertEquals(expectedData, "0")
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-21-1529 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayComplete("EU/1/21/1529")

        assertEquals(expectedData, "365")
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-20-1525 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayNotComplete("EU/1/20/1525")

        assertEquals(expectedData, "15")
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-20-1525 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayNotComplete("EU/1/20/1525")

        assertEquals(expectedData, "365")
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-20-1525 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineStartDayComplete("EU/1/20/1525")

        assertEquals(expectedData, "15")
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-20-1525 vaccine type`() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedData = viewModel.getVaccineEndDayComplete("EU/1/20/1525")

        assertEquals(expectedData, "365")
    }

    @Test
    fun getVaccineStartDayCompleteUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getVaccineStartDayCompleteUnified("IT").toLong()
        val expectedDataNOTIT = viewModel.getVaccineStartDayCompleteUnified("US").toLong()

        assertEquals(expectedDataIT, 0L)
        assertEquals(expectedDataNOTIT, 0L)
    }

    @Test
    fun getVaccineEndDayCompleteUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getVaccineEndDayCompleteUnified("IT")
        val expectedDataNOTIT = viewModel.getVaccineEndDayCompleteUnified("US")

        assertEquals(expectedDataIT, "180")
        assertEquals(expectedDataNOTIT, "270")
    }

    @Test
    fun getVaccineStartDayBoosterUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getVaccineStartDayBoosterUnified("IT")
        val expectedDataNOTIT = viewModel.getVaccineStartDayBoosterUnified("US")

        assertEquals(expectedDataIT, "0")
        assertEquals(expectedDataNOTIT, "0")
    }

    @Test
    fun getVaccineEndDayBoosterUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getVaccineEndDayBoosterUnified("IT")
        val expectedDataNOTIT = viewModel.getVaccineEndDayBoosterUnified("US")

        assertEquals(expectedDataIT, "180")
        assertEquals(expectedDataNOTIT, "270")
    }

    @Test
    fun getRecoveryCertStartDayUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getRecoveryCertStartDayUnified("IT")
        val expectedDataNOTIT = viewModel.getRecoveryCertStartDayUnified("US")

        assertEquals(expectedDataIT, "0")
        assertEquals(expectedDataNOTIT, "0")
    }

    @Test
    fun getRecoveryCertEndDayUnified() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val expectedDataIT = viewModel.getRecoveryCertEndDayUnified("IT")
        val expectedDataNOTIT = viewModel.getRecoveryCertEndDayUnified("US")

        assertEquals(expectedDataIT, "180")
        assertEquals(expectedDataNOTIT, "270")
    }


    @Test
    fun getVaccineEndDayCompleteUnified_foreignCertification_expectedSetting() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)

        val model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_VALID_NOT_IT
            ), CertificateModel::class.java
        )

        val expectedSetting = viewModel.getVaccineEndDayCompleteUnified(model.vaccinations!!.last()!!.countryOfVaccination)
        assertEquals(expectedSetting, "270")
    }

    private fun String.base64ToX509Certificate(): X509Certificate? {
        val decoded = Base64.decode(this, 2)
        val inputStream = ByteArrayInputStream(decoded)
        return CertificateFactory.getInstance("X.509").generateCertificate(inputStream) as? X509Certificate
    }

}