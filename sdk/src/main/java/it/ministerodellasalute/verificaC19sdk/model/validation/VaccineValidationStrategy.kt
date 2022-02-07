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
 *  Created by nicolamcornelio on 1/19/22, 10:03 AM
 */

package it.ministerodellasalute.verificaC19sdk.model.validation

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.model.*
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
        val countryCode = if (scanMode == ScanMode.STANDARD) vaccination.countryOfVaccination else Country.IT.value

        if (!ruleSet.hasRulesForVaccine(vaccineType)) return CertificateStatus.NOT_VALID
        if (vaccination.isNotAllowed()) return CertificateStatus.NOT_VALID

        return try {
            val startDate: LocalDate = retrieveStartDate(vaccination, ruleSet, countryCode)
            val endDate: LocalDate? = retrieveEndDate(vaccination, ruleSet, countryCode, scanMode)
            Log.d("VaccineDates", "Start: $startDate End: $endDate")

            when {
                LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                else -> {
                    validateWithScanMode(vaccination, scanMode)
                }
            }
        } catch (e: Exception) {
            CertificateStatus.NOT_EU_DCC
        }
    }

    private fun validateWithScanMode(vaccination: VaccinationModel, scanMode: ScanMode?): CertificateStatus {
        return when {
            vaccination.isComplete() -> when (scanMode) {
                ScanMode.BOOSTER -> {
                    if (vaccination.isBooster()) return CertificateStatus.VALID
                    return CertificateStatus.TEST_NEEDED
                }
                else -> return CertificateStatus.VALID
            }
            vaccination.isNotComplete() -> if (ScanMode.BOOSTER == scanMode || ScanMode.SCHOOL == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
            else -> return CertificateStatus.NOT_EU_DCC
        }
    }

    private fun retrieveStartDate(
        vaccination: VaccinationModel,
        ruleSet: RuleSet,
        countryCode: String
    ): LocalDate {
        vaccination.run {
            val dateOfVaccination = dateOfVaccination.toLocalDate()
            return when {
                isComplete() -> {
                    val startDaysToAdd =
                        if (isBooster()) ruleSet.getVaccineStartDayBoosterUnified(countryCode)
                        else ruleSet.getVaccineStartDayCompleteUnified(countryCode, medicinalProduct)
                    dateOfVaccination.plusDays(startDaysToAdd)
                }
                isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineStartDayNotComplete(medicinalProduct))
                else -> dateOfVaccination
            }
        }
    }

    private fun retrieveEndDate(
        vaccination: VaccinationModel,
        ruleSet: RuleSet,
        countryCode: String,
        scanMode: ScanMode?
    ): LocalDate? {
        vaccination.run {
            val dateOfVaccination = dateOfVaccination.toLocalDate()
            return when {
                isComplete() -> {
                    val endDaysToAdd =
                        when {
                            isBooster() -> ruleSet.getVaccineEndDayBoosterUnified(countryCode)
                            scanMode == ScanMode.SCHOOL -> ruleSet.getRecoveryCertEndDaySchool()
                            else -> ruleSet.getVaccineEndDayCompleteUnified(countryCode)
                        }
                    dateOfVaccination.plusDays(endDaysToAdd)
                }
                isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineEndDayNotComplete(medicinalProduct))
                else -> null
            }
        }
    }

}