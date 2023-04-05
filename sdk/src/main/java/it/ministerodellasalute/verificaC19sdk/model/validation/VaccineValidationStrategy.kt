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

import it.ministerodellasalute.verificaC19sdk.model.*
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
import java.time.LocalDate

class VaccineValidationStrategy : ValidationStrategy {

    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate

    /**
     *
     * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
     * the proper status as [CertificateStatus].
     *
     */
    override fun checkCertificate(
        certificateModel: CertificateModel,
        ruleSet: RuleSet
    ): CertificateStatus {
        val vaccine = certificateModel.vaccinations?.last()!!
        if (vaccine.isNotComplete() && !ruleSet.isEMA(
                vaccine.medicinalProduct,
                vaccine.countryOfVaccination
            )
        ) return CertificateStatus.NOT_VALID
        return try {
            validateWithScanMode(certificateModel, ruleSet)
        } catch (e: Exception) {
            CertificateStatus.NOT_EU_DCC
        }
    }

    private fun validateWithScanMode(
        certificateModel: CertificateModel,
        ruleSet: RuleSet
    ): CertificateStatus {
        return when (certificateModel.scanMode) {
            ScanMode.STANDARD -> vaccineStandardStrategy(certificateModel, ruleSet)
            else -> {
                CertificateStatus.NOT_EU_DCC
            }
        }
    }

    private fun vaccineStandardStrategy(
        certificateModel: CertificateModel,
        ruleSet: RuleSet
    ): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!

        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()
        startDate =
            when {
                vaccination.isComplete() -> {
                    val startDaysToAdd =
                        if (vaccination.isBooster()) ruleSet.getVaccineStartDayBoosterUnified(
                            Country.IT.value
                        )
                        else ruleSet.getVaccineStartDayCompleteUnified(
                            Country.IT.value,
                            vaccination.medicinalProduct
                        )
                    dateOfVaccination.plusDays(startDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(
                    ruleSet.getVaccineStartDayNotComplete(
                        vaccination.medicinalProduct
                    )
                )
                else -> dateOfVaccination
            }
        endDate =
            when {
                vaccination.isComplete() -> {
                    val endDaysToAdd =
                        when {
                            vaccination.isBooster() -> ruleSet.getVaccineEndDayBoosterUnified(
                                Country.IT.value
                            )
                            else -> ruleSet.getVaccineEndDayCompleteUnified(Country.IT.value)
                        }
                    dateOfVaccination.plusDays(endDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(
                    ruleSet.getVaccineEndDayNotComplete(
                        vaccination.medicinalProduct
                    )
                )
                else -> dateOfVaccination
            }

        return when {
            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
            LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
            !ruleSet.isEMA(
                vaccination.medicinalProduct,
                vaccination.countryOfVaccination
            ) -> CertificateStatus.NOT_VALID
            else -> CertificateStatus.VALID
        }
    }
}