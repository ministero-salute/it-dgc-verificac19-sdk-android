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

package it.ministerodellasalute.verificaC19sdk

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.data.local.ScanMode
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.clearExtraTime
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.time.LocalDate

class RecoveryValidationStrategy : ValidationStrategy {

    private lateinit var validationRules: Array<Rule>

    override fun checkCertificate(certificateModel: CertificateModel, validationRules: Array<Rule>): CertificateStatus {

        this.validationRules = validationRules
        val it: List<RecoveryModel> = certificateModel.recoveryStatements!!
        val scanMode = certificateModel.scanMode
        val certificate = certificateModel.certificate

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

    private fun getRecoveryCertStartDay(): String {
        return validationRules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getRecoveryCertPVStartDay(): String {
        return validationRules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getRecoveryCertEndDay(): String {
        return validationRules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY.value }?.value
            ?: run {
                ""
            }
    }

    private fun getRecoveryCertPvEndDay(): String {
        return validationRules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY.value }?.value
            ?: run {
                ""
            }
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
}