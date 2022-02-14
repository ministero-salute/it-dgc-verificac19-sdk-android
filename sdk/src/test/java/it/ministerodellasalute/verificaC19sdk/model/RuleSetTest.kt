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
import io.mockk.every
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet.Companion.NO_VALUE
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.utils.mock.MockDataUtils
import it.ministerodellasalute.verificaC19sdk.utils.mock.ServiceMocks
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleSetTest {

    companion object {
        private const val CERTIFICATE_MODEL_VACCINATION_VALID_NOT_IT = "certificate_model_vaccination_valid_NOT_IT.json"
    }

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var ruleSet: RuleSet

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        ruleSet = RuleSet(ServiceMocks.getVerificationRulesStringResponse())
    }


    @Test
    fun getRecoveryCertStartDay() {
        val expectedData = ruleSet.getRecoveryCertStartDay()
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun getRecoveryCertEndDay() {
        val expectedData = ruleSet.getRecoveryCertEndDay()
        Assert.assertEquals(expectedData, 180L)
    }

    @Test
    fun getRapidTestStartHour() {
        val expectedData = ruleSet.getRapidTestStartHour()
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun getRapidTestEndHour() {
        val expectedData = ruleSet.getRapidTestEndHour()
        Assert.assertEquals(expectedData, 48L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for PFIZER`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for PFIZER`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for PFIZER`() {
        val expectedData = ruleSet.getVaccineStartDayComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for PFIZER`() {
        val expectedData = ruleSet.getVaccineEndDayComplete(MedicinalProduct.PFIZER)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for MODERNA`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for MODERNA`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for MODERNA`() {
        val expectedData = ruleSet.getVaccineStartDayComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for MODERNA`() {
        val expectedData = ruleSet.getVaccineEndDayComplete(MedicinalProduct.MODERNA)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for ASTRAZENECA`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for ASTRAZENECA`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 84L)
    }

    @Test
    fun `getVaccineStartDayComplete for ASTRAZENECA`() {
        val expectedData = ruleSet.getVaccineStartDayComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for ASTRAZENECA`() {
        val expectedData = ruleSet.getVaccineEndDayComplete(MedicinalProduct.ASTRAZENECA)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for JANSEN`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for JANSEN`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayComplete for JANSEN`() {
        val expectedData = ruleSet.getVaccineStartDayComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayComplete for JANSEN`() {
        val expectedData = ruleSet.getVaccineEndDayComplete(MedicinalProduct.JANSEN)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun getVaccineStartDayCompleteUnified() {
        val expectedDataIT = ruleSet.getVaccineStartDayCompleteUnified(Country.IT.value, "")
        val expectedDataNOTIT = ruleSet.getVaccineStartDayCompleteUnified(Country.SM.value, "")
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getVaccineEndDayCompleteUnified() {
        val expectedDataIT = ruleSet.getVaccineEndDayCompleteUnified(Country.IT.value)
        val expectedDataNOTIT = ruleSet.getVaccineEndDayCompleteUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, 180L)
        Assert.assertEquals(expectedDataNOTIT, 270L)
    }

    @Test
    fun getVaccineStartDayBoosterUnified() {
        val expectedDataIT = ruleSet.getVaccineStartDayBoosterUnified(Country.IT.value)
        val expectedDataNOTIT = ruleSet.getVaccineStartDayBoosterUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getVaccineEndDayBoosterUnified() {
        val expectedDataIT = ruleSet.getVaccineEndDayBoosterUnified(Country.IT.value)
        val expectedDataNOTIT = ruleSet.getVaccineEndDayBoosterUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, 180)
        Assert.assertEquals(expectedDataNOTIT, 270L)
    }

    @Test
    fun getRecoveryCertStartDayUnified() {
        val expectedDataIT = ruleSet.getRecoveryCertStartDayUnified(Country.IT.value)
        val expectedDataNOTIT = ruleSet.getRecoveryCertStartDayUnified(Country.SM.value)
        Assert.assertEquals(expectedDataIT, NO_VALUE)
        Assert.assertEquals(expectedDataNOTIT, NO_VALUE)
    }

    @Test
    fun getRecoveryCertEndDayUnified() {
        val expectedDataIT = ruleSet.getRecoveryCertEndDayUnified(Country.IT.value)
        val expectedDataNOTIT = ruleSet.getRecoveryCertEndDayUnified(Country.SM.value)
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
        val expectedSetting = ruleSet.getVaccineEndDayCompleteUnified(model.vaccinations!!.last().countryOfVaccination)
        Assert.assertEquals(expectedSetting, 270L)
    }
}