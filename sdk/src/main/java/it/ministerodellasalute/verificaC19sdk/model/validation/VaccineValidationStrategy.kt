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
 *  Created by lucarinzivillo on 26/01/22, 12:51
 */

package it.ministerodellasalute.verificaC19sdk.model.validation

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.model.ScanMode
import it.ministerodellasalute.verificaC19sdk.model.CertificateModel
import it.ministerodellasalute.verificaC19sdk.model.CertificateStatus
import it.ministerodellasalute.verificaC19sdk.model.VaccinationModel
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
import java.time.LocalDate

class VaccineValidationStrategy : ValidationStrategy {


    /**
     *
     * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
     * the proper status as [CertificateStatus].
     *
     */
    override fun checkCertificate(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val scanMode = certificateModel.scanMode
        val vaccination = certificateModel.vaccinations!!.last()
        val vaccineType = vaccination.medicinalProduct

        if (!ruleSet.hasRulesForVaccine(vaccineType)) return CertificateStatus.NOT_VALID
        if (vaccination.isNotAllowed()) return CertificateStatus.NOT_VALID

        try {
            val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()
            val startDate: LocalDate? = when {
                vaccination.isComplete() && vaccination.isNotBooster() -> dateOfVaccination.plusDays(ruleSet.getVaccineStartDayComplete(vaccineType))
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineStartDayNotComplete(vaccineType))
                else -> dateOfVaccination
            }
            val endDate: LocalDate? = when {
                vaccination.isComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineEndDayComplete(vaccineType))
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineEndDayNotComplete(vaccineType))
                else -> null
            }
            Log.d("ValidityDates", "Start: $startDate End: $endDate")
            when {
                vaccination.isNotComplete() -> {
                    return when {
                        LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> if (ScanMode.BOOSTER == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
                    }
                }
                vaccination.isComplete() -> {
                    return when {
                        LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> {
                            when (scanMode) {
                                ScanMode.BOOSTER -> {
                                    if (vaccination.isNotBooster()) return CertificateStatus.TEST_NEEDED
                                    return CertificateStatus.VALID
                                }
                                else -> return CertificateStatus.VALID
                            }
                        }
                    }
                }
                else -> CertificateStatus.NOT_VALID
            }
        } catch (e: Exception) {
            return CertificateStatus.NOT_EU_DCC
        }
        return CertificateStatus.NOT_EU_DCC
    }

}