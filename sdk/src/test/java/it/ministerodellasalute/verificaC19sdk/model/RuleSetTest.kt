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
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet.Companion.NO_VALUE
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet
import it.ministerodellasalute.verificaC19sdk.utils.mock.ServiceMocks
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RuleSetTest {

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
    fun `getVaccineStartDayNotComplete for EU-1-20-1528 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete("EU/1/20/1528")
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-20-1528 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete("EU/1/20/1528")
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-20-1528 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayComplete("EU/1/20/1528")
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-20-1528 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayComplete("EU/1/20/1528")
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-20-1507 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete("EU/1/20/1507")
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-20-1507 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete("EU/1/20/1507")
        Assert.assertEquals(expectedData, 42L)
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-20-1507 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayComplete("EU/1/20/1507")
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-20-1507 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayComplete("EU/1/20/1507")
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for EU-1-21-1529 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete("EU/1/21/1529")
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for EU-1-21-1529 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete("EU/1/21/1529")
        Assert.assertEquals(expectedData, 84L)
    }

    @Test
    fun `getVaccineStartDayComplete for EU-1-21-1529 vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayComplete("EU/1/21/1529")
        Assert.assertEquals(expectedData, NO_VALUE)
    }

    @Test
    fun `getVaccineEndDayComplete for EU-1-21-1529 vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayComplete("EU/1/21/1529")
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayNotComplete for JOHNSON vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayNotComplete(MedicinalProduct.JOHNSON)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayNotComplete for JOHNSON vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayNotComplete(MedicinalProduct.JOHNSON)
        Assert.assertEquals(expectedData, 365L)
    }

    @Test
    fun `getVaccineStartDayComplete for JOHNSON vaccineType`() {
        val expectedData = ruleSet.getVaccineStartDayComplete(MedicinalProduct.JOHNSON)
        Assert.assertEquals(expectedData, 15L)
    }

    @Test
    fun `getVaccineEndDayComplete for JOHNSON vaccineType`() {
        val expectedData = ruleSet.getVaccineEndDayComplete(MedicinalProduct.JOHNSON)
        Assert.assertEquals(expectedData, 365L)
    }
}