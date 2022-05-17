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
 *  Created by kaizen-7 on 26/01/22, 11:20
 */

package it.ministerodellasalute.verificaC19sdk.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import it.ministerodellasalute.verificaC19sdk.model.validation.Settings.Companion.NO_VALUE
import it.ministerodellasalute.verificaC19sdk.model.validation.Settings
import it.ministerodellasalute.verificaC19sdk.utils.mock.MockDataUtils
import it.ministerodellasalute.verificaC19sdk.utils.mock.ServiceMocks
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsTest {

    companion object {
        private const val CERTIFICATE_MODEL_VACCINATION_VALID_NOT_IT = "certificate_model_vaccination_valid_NOT_IT.json"
    }

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var settings: Settings

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        settings = Settings(ServiceMocks.getVerificationRulesStringResponse())
    }


    @Test
    fun getRecoveryCertStartDay() {
        val expectedData = settings.getRecoveryCertStartDay()
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun getRecoveryCertEndDay() {
        val expectedData = settings.getRecoveryCertEndDay()
        Assert.assertEquals(expectedData, 180L)
    }

    @Test
    fun getRapidTestStartHour() {
        val expectedData = settings.getRapidTestStartHour()
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun getRapidTestEndHour() {
        val expectedData = settings.getRapidTestEndHour()
        Assert.assertEquals(expectedData, 48L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for PFIZER`() {
        val expectedData = settings.getVaccineStartDayNotComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for PFIZER`() {
        val expectedData = settings.getVaccineEndDayNotComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for PFIZER`() {
        val expectedData = settings.getVaccineStartDayComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for PFIZER`() {
        val expectedData = settings.getVaccineEndDayComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for MODERNA`() {
        val expectedData = settings.getVaccineStartDayNotComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for MODERNA`() {
        val expectedData = settings.getVaccineEndDayNotComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for MODERNA`() {
        val expectedData = settings.getVaccineStartDayComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for MODERNA`() {
        val expectedData = settings.getVaccineEndDayComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for ASTRAZENECA`() {
        val expectedData = settings.getVaccineStartDayNotComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for ASTRAZENECA`() {
        val expectedData = settings.getVaccineEndDayNotComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 84L)
    }

    @Test
    fun `getVaccineStartDayComplete for ASTRAZENECA`() {
        val expectedData = settings.getVaccineStartDayComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for ASTRAZENECA`() {
        val expectedData = settings.getVaccineEndDayComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for JANSEN`() {
        val expectedData = settings.getVaccineStartDayNotComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for JANSEN`() {
        val expectedData = settings.getVaccineEndDayNotComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayComplete for JANSEN`() {
        val expectedData = settings.getVaccineStartDayComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayComplete for JANSEN`() {
        val expectedData = settings.getVaccineEndDayComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun getVaccineStartDayCompleteUnified() {
        val expectedDataIT = settings.getVaccineStartDayCompleteUnified(Country.IT.value, "")
        val expectedDataNOTIT = settings.getVaccineStartDayCompleteUnified(Country.SM.value, "")
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getVaccineEndDayCompleteUnified() {
        val expectedDataIT = settings.getVaccineEndDayCompleteUnified(Country.IT.value)
        val expectedDataNOTIT = settings.getVaccineEndDayCompleteUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, 180L)
        Assert.assertEquals(expectedDataNOTIT, 270L)
    }

    @Test
    fun getVaccineStartDayBoosterUnified() {
        val expectedDataIT = settings.getVaccineStartDayBoosterUnified(Country.IT.value)
        val expectedDataNOTIT = settings.getVaccineStartDayBoosterUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getVaccineEndDayBoosterUnified() {
        val expectedDataIT = settings.getVaccineEndDayBoosterUnified(Country.IT.value)
        val expectedDataNOTIT = settings.getVaccineEndDayBoosterUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, 180)
        Assert.assertEquals(expectedDataNOTIT, 270L)
    }

    @Test
    fun getRecoveryCertStartDayUnified() {
        val expectedDataIT = settings.getRecoveryCertStartDayUnified(Country.IT.value)
        val expectedDataNOTIT = settings.getRecoveryCertStartDayUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getRecoveryCertEndDayUnified() {
        val expectedDataIT = settings.getRecoveryCertEndDayUnified(Country.IT.value)
        val expectedDataNOTIT = settings.getRecoveryCertEndDayUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, 180L)
        Assert.assertEquals(expectedDataNOTIT, 270L)
    }


    @Test
    fun getVaccineEndDayCompleteUnified_foreignCertification_expectedSetting() {
        val model = MockDataUtils.GSON.fromJson(
            MockDataUtils.readFile(
                CERTIFICATE_MODEL_VACCINATION_VALID_NOT_IT
            ), CertificateModel::class.java
        )
        val expectedSetting = settings.getVaccineEndDayCompleteUnified(model.vaccinations!!.last().countryOfVaccination)
        Assert.assertEquals(expectedSetting, 270L)
    }
}