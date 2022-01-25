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
 *  Created by nicolamcornelio on 1/19/22, 10:35 AM
 */

package it.ministerodellasalute.verificaC19sdk

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.data.local.ScanMode
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.*
import java.time.LocalDateTime
import java.time.OffsetDateTime

class TestValidationStrategy : ValidationStrategy {

    private lateinit var validationRules: Array<Rule>

    /**
     *
     * This method checks the given tests passed as a [List] of [TestModel] and returns the proper
     * status as [CertificateStatus].
     *
     */
    override fun checkCertificate(certificateModel: CertificateModel, validationRules: Array<Rule>): CertificateStatus {
        this.validationRules = validationRules
        val it: List<TestModel> = certificateModel.tests!!
        val scanMode = certificateModel.scanMode

        if (scanMode == ScanMode.BOOSTER || scanMode == ScanMode.STRENGTHENED) return CertificateStatus.NOT_VALID

        if (it.last().resultType == TestResult.DETECTED) {
            return CertificateStatus.NOT_VALID
        }
        try {
            val odtDateTimeOfCollection = OffsetDateTime.parse(it.last().dateTimeOfCollection)
            val ldtDateTimeOfCollection = odtDateTimeOfCollection.toLocalDateTime()

            val testType = it.last().typeOfTest

            val startDate: LocalDateTime
            val endDate: LocalDateTime

            when (testType) {
                TestType.MOLECULAR.value -> {
                    startDate = ldtDateTimeOfCollection.plusHours(getMolecularTestStartHour())
                    endDate = ldtDateTimeOfCollection.plusHours(getMolecularTestEndHour())
                }
                TestType.RAPID.value -> {
                    startDate = ldtDateTimeOfCollection.plusHours(getRapidTestStartHour())
                    endDate = ldtDateTimeOfCollection.plusHours(getRapidTestEndHour())
                }
                else -> {
                    return CertificateStatus.NOT_VALID
                }
            }

            Log.d("TestDates", "Start: $startDate End: $endDate")
            return when {
                startDate.isAfter(LocalDateTime.now()) -> CertificateStatus.NOT_VALID_YET
                LocalDateTime.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                else -> CertificateStatus.VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
    }

    private fun getMolecularTestStartHour(): Long {
        return validationRules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_START_HOUR.value }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getMolecularTestEndHour(): Long {
        return validationRules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_END_HOUR.value }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getRapidTestStartHour(): Long {
        return validationRules.find { it.name == ValidationRulesEnum.RAPID_TEST_START_HOUR.value }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getRapidTestEndHour(): Long {
        return validationRules.find { it.name == ValidationRulesEnum.RAPID_TEST_END_HOUR.value }?.value?.toLong()
            ?: run {
                0L
            }
    }
}