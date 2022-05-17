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

package it.ministerodellasalute.verificaC19sdk.model.validation

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.*
import java.time.LocalDateTime
import java.time.OffsetDateTime

class TestValidationStrategy : ValidationStrategy {


    /**
     *
     * This method checks the given tests passed as a [List] of [TestModel] and returns the proper
     * status as [CertificateStatus].
     *
     */
    override fun checkCertificate(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val test: TestModel = certificateModel.tests!!.first()
        val scanMode = certificateModel.scanMode
        val isADoubleScanBoosterTest = test.isPreviousScanModeBooster
        val isTestNotAllowed = scanMode == ScanMode.BOOSTER || scanMode == ScanMode.STRENGTHENED

        if (test.resultType == TestResult.DETECTED) {
            return CertificateStatus.NOT_VALID
        }
        try {
            val odtDateTimeOfCollection = OffsetDateTime.parse(test.dateTimeOfCollection)
            val ldtDateTimeOfCollection = odtDateTimeOfCollection.toLocalDateTime()

            val testType = test.typeOfTest

            val startDate: LocalDateTime
            val endDate: LocalDateTime

            when (testType) {
                TestType.MOLECULAR.value -> {
                    startDate = ldtDateTimeOfCollection.plusHours(settings.getMolecularTestStartHour())
                    endDate =
                        if (scanMode == ScanMode.DOUBLE_SCAN && isADoubleScanBoosterTest)
                            ldtDateTimeOfCollection.plusHours(settings.getRapidTestEndHour())
                        else
                            ldtDateTimeOfCollection.plusHours(settings.getMolecularTestEndHour())
                }
                TestType.RAPID.value -> {
                    startDate = ldtDateTimeOfCollection.plusHours(settings.getRapidTestStartHour())
                    endDate = ldtDateTimeOfCollection.plusHours(settings.getRapidTestEndHour())
                }
                else -> {
                    return CertificateStatus.NOT_VALID
                }
            }
            Log.d("TestDates", "Start: $startDate End: $endDate")
            return when {
                LocalDateTime.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                LocalDateTime.now().isAfter(endDate) -> CertificateStatus.EXPIRED
                isTestNotAllowed -> CertificateStatus.NOT_VALID
                else -> CertificateStatus.VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
    }

}