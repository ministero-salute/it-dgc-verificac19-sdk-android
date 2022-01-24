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

package it.ministerodellasalute.verificaC19sdk

import android.util.Log
import it.ministerodellasalute.verificaC19sdk.data.local.MedicinalProduct
import it.ministerodellasalute.verificaC19sdk.data.local.ScanMode
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.CertificateModel
import it.ministerodellasalute.verificaC19sdk.model.CertificateStatus
import it.ministerodellasalute.verificaC19sdk.model.VaccinationModel
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.clearExtraTime
import java.time.LocalDate

class VaccineValidationStrategy : ValidationStrategy {

    private lateinit var validationRules: Array<Rule>

    /**
     *
     * This method checks the given vaccinations passed as a [List] of [VaccinationModel] and returns
     * the proper status as [CertificateStatus].
     *
     */
    override fun checkCertificate(certificateModel: CertificateModel, validationRules: Array<Rule>): CertificateStatus {
        this.validationRules = validationRules
        // Check if vaccine is present in setting list; otherwise, return not valid
        val vaccinations: List<VaccinationModel> = certificateModel.vaccinations!!
        val scanMode = certificateModel.scanMode

        val vaccineEndDayComplete = getVaccineEndDayComplete(vaccinations.last().medicinalProduct)
        val isValid = vaccineEndDayComplete.isNotEmpty()
        if (!isValid) return CertificateStatus.NOT_VALID
        val isSputnikNotFromSanMarino =
            vaccinations.last().medicinalProduct == "Sputnik-V" && vaccinations.last().countryOfVaccination != "SM"
        if (isSputnikNotFromSanMarino) return CertificateStatus.NOT_VALID

        try {
            when {
                vaccinations.last().doseNumber < vaccinations.last().totalSeriesOfDoses -> {
                    val startDate: LocalDate =
                        LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineStartDayNotComplete(vaccinations.last().medicinalProduct))
                                    .toLong()
                            )

                    val endDate: LocalDate =
                        LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineEndDayNotComplete(vaccinations.last().medicinalProduct))
                                    .toLong()
                            )
                    Log.d("dates", "start:$startDate end: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now()
                            .isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> if (ScanMode.BOOSTER == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
                    }
                }
                vaccinations.last().doseNumber >= vaccinations.last().totalSeriesOfDoses -> {
                    val startDate: LocalDate
                    val endDate: LocalDate
                    if (vaccinations.last().medicinalProduct == MedicinalProduct.JOHNSON && (vaccinations.last().doseNumber > vaccinations.last().totalSeriesOfDoses) ||
                        (vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber >= 2)
                    ) {
                        startDate = LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))

                        endDate = LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))
                            .plusDays(
                                Integer.parseInt(getVaccineEndDayComplete(vaccinations.last().medicinalProduct))
                                    .toLong()
                            )
                    } else {
                        startDate =
                            LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))
                                .plusDays(
                                    Integer.parseInt(getVaccineStartDayComplete(vaccinations.last().medicinalProduct))
                                        .toLong()
                                )

                        endDate =
                            LocalDate.parse(clearExtraTime(vaccinations.last().dateOfVaccination))
                                .plusDays(
                                    Integer.parseInt(getVaccineEndDayComplete(vaccinations.last().medicinalProduct))
                                        .toLong()
                                )
                    }
                    Log.d("dates", "start:$startDate end: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now()
                            .isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> {
                            when (scanMode) {
                                ScanMode.BOOSTER -> {
                                    if (vaccinations.last().medicinalProduct == MedicinalProduct.JOHNSON) {
                                        if (vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber < 2) return CertificateStatus.TEST_NEEDED
                                    } else {
                                        if ((vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber < 3))
                                            return CertificateStatus.TEST_NEEDED
                                    }
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

    private fun getVaccineStartDayNotComplete(vaccineType: String): String {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    private fun getVaccineEndDayNotComplete(vaccineType: String): String {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    private fun getVaccineStartDayComplete(vaccineType: String): String {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    private fun getVaccineEndDayComplete(vaccineType: String): String {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }
}