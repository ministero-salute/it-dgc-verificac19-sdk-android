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
 *  Created by nicolamcornelio on 1/19/22, 10:34 AM
 */

package it.ministerodellasalute.verificaC19sdk.model.validation

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
import java.security.cert.Certificate
import java.time.LocalDate

class RecoveryValidationStrategy : ValidationStrategy {

    override fun checkCertificate(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val recovery: RecoveryModel = certificateModel.recoveryStatements!!.first()
        val scanMode = certificateModel.scanMode
        val certificate = certificateModel.certificate

        val countryCode = if (scanMode == ScanMode.STANDARD) recovery.country else Country.IT.value

        val recoveryBis = recovery.isRecoveryBis(certificate)
        val startDaysToAdd = if (recoveryBis) ruleSet.getRecoveryCertPVStartDay() else ruleSet.getRecoveryCertStartDayUnified(countryCode)

        val endDaysToAdd = when {
            scanMode == ScanMode.SCHOOL -> ruleSet.getRecoveryCertEndDaySchool()
            recoveryBis -> ruleSet.getRecoveryCertPvEndDay()
            else -> ruleSet.getRecoveryCertEndDayUnified(countryCode)
        }

        val certificateValidUntil = recovery.certificateValidUntil.toLocalDate()
        val dateOfFirstPositiveTest = recovery.dateOfFirstPositiveTest.toLocalDate().plusDays(endDaysToAdd)


        try {
            val startDate: LocalDate = recovery.certificateValidFrom.toLocalDate()

            val endDate: LocalDate =
                if (scanMode == ScanMode.SCHOOL)
                    if (certificateValidUntil.isBefore(dateOfFirstPositiveTest)) certificateValidUntil else dateOfFirstPositiveTest
                else
                    startDate.plusDays(endDaysToAdd)

            Log.d("RecoveryDates", "Start: $startDate End: $endDate")
            return when {
                LocalDate.now().isBefore(startDate.plusDays(startDaysToAdd)) -> CertificateStatus.NOT_VALID_YET
                LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                else -> return if (scanMode == ScanMode.BOOSTER) CertificateStatus.TEST_NEEDED else CertificateStatus.VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_VALID
        }
    }

}