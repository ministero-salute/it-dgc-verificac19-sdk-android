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
import it.ministerodellasalute.verificaC19sdk.data.local.prefs.Preferences
import it.ministerodellasalute.verificaC19sdk.data.repository.VerifierRepository
import it.ministerodellasalute.verificaC19sdk.di.DispatcherProvider
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
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
    private val nowLocalDateTime = LocalDateTime.of(2021, Month.OCTOBER, 19, 14, 57, 54, 0);


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
        val ruleSet = RuleSet(preferences.validationRulesJson)
        every { LocalDate.now() } returns nowLocalDate

        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_RECOVERY_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID)
    }

    @Test
    fun getCertificateStatusVaccination() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)
        every { LocalDate.now() } returns nowLocalDate
        val ruleSet = RuleSet(preferences.validationRulesJson)
        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID)
    }

    @Test
    fun getCertificateStatusTest() {
        val response = ServiceMocks.getVerificationRulesStringResponse()
        every { preferences.validationRulesJson }.returns(response)
        every { LocalDateTime.now() } returns nowLocalDateTime
        val ruleSet = RuleSet(preferences.validationRulesJson)

        var model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_VALID
            ), CertificateModel::class.java
        )
        var result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.VALID)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_NOT_VALID_YET
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID_YET)

        model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_TEST_NOT_VALID
            ), CertificateModel::class.java
        )
        result = viewModel.getCertificateStatus(model, ruleSet)
        assertEquals(result, CertificateStatus.NOT_VALID)

    }
}