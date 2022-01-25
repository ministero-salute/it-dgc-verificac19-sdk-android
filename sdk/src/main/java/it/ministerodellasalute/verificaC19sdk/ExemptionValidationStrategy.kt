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
 *  Created by nicolamcornelio on 1/19/22, 12:05 PM
 */

package it.ministerodellasalute.verificaC19sdk

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.data.local.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.CertificateModel
import it.ministerodellasalute.verificaC19sdk.model.CertificateStatus
import it.ministerodellasalute.verificaC19sdk.model.Exemption
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility
import java.time.LocalDate

class ExemptionValidationStrategy : ValidationStrategy {

    /**
     * This method checks the [Exemption] and returns a proper [CertificateStatus]
     * after checking the validity start and end dates.
     */
    override fun checkCertificate(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val exemptions: List<Exemption> = certificateModel.exemptions!!
        val scanMode = certificateModel.scanMode

        try {
            val startDate: LocalDate = LocalDate.parse(TimeUtility.clearExtraTime(exemptions.last().certificateValidFrom))
            val endDate: LocalDate? = exemptions.last().certificateValidUntil?.let {
                LocalDate.parse(TimeUtility.clearExtraTime(it))
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
}