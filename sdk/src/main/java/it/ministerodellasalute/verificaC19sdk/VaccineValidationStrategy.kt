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
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
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
        val vaccinations: List<VaccinationModel> = certificateModel.vaccinations!!
        val scanMode = certificateModel.scanMode
        val vaccineType = vaccinations.last().medicinalProduct

        if (isVaccineNotInSettings(vaccineType)) return CertificateStatus.NOT_VALID
        if (checkForSputnik(vaccineType, vaccinations)) return CertificateStatus.NOT_VALID

        try {
            val dateOfVaccination = vaccinations.last().dateOfVaccination
            val startDate: LocalDate
            val endDate: LocalDate
            when {
                isVaccineNotComplete(vaccinations) -> {
                    startDate = dateOfVaccination.toLocalDate().plusDays(getVaccineStartDayNotComplete(vaccineType))
                    endDate = dateOfVaccination.toLocalDate().plusDays(getVaccineEndDayNotComplete(vaccineType))

                    Log.d("VaccineNotCompleteDates", "Start: $startDate End: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> if (ScanMode.BOOSTER == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
                    }
                }

                isVaccineComplete(vaccinations) -> {
                    if (isJohnsonVaccineComplete(vaccineType, vaccinations)) {
                        startDate = dateOfVaccination.toLocalDate()
                        endDate = dateOfVaccination.toLocalDate().plusDays(getVaccineEndDayComplete(vaccineType))
                    } else {
                        startDate = dateOfVaccination.toLocalDate().plusDays(getVaccineStartDayComplete(vaccineType))
                        endDate = dateOfVaccination.toLocalDate().plusDays(getVaccineEndDayComplete(vaccineType))
                    }

                    Log.d("VaccineCompleteDates", "Start:$startDate End: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> {
                            when (scanMode) {
                                ScanMode.BOOSTER -> {
                                    if (isJohnsonVaccineNotBooster(vaccineType, vaccinations)) return CertificateStatus.TEST_NEEDED
                                    else if (isVaccineNotBooster(vaccinations)) return CertificateStatus.TEST_NEEDED
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

    private fun isVaccineNotInSettings(vaccineType: String): Boolean {
        val vaccineEndDayComplete = getVaccineEndDayComplete(vaccineType).toString()
        val isValid = vaccineEndDayComplete.isNotEmpty()
        if (!isValid) return true
        return false
    }

    private fun isVaccineNotBooster(vaccinations: List<VaccinationModel>) =
        (vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber < 3)

    private fun isJohnsonVaccineNotBooster(
        vaccineType: String,
        vaccinations: List<VaccinationModel>
    ) =
        vaccineType == MedicinalProduct.JOHNSON && vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber < 2

    private fun isJohnsonVaccineComplete(
        vaccineType: String,
        vaccinations: List<VaccinationModel>
    ) = vaccineType == MedicinalProduct.JOHNSON && (vaccinations.last().doseNumber > vaccinations.last().totalSeriesOfDoses) ||
            (vaccinations.last().doseNumber == vaccinations.last().totalSeriesOfDoses && vaccinations.last().doseNumber >= 2)

    private fun checkForSputnik(
        vaccineType: String,
        vaccinations: List<VaccinationModel>
    ): Boolean {
        val isSputnikNotFromSanMarino =
            vaccineType == "Sputnik-V" && vaccinations.last().countryOfVaccination != "SM"
        if (isSputnikNotFromSanMarino) return true
        return false
    }

    private fun isVaccineComplete(vaccinations: List<VaccinationModel>) =
        vaccinations.last().doseNumber >= vaccinations.last().totalSeriesOfDoses

    private fun isVaccineNotComplete(vaccinations: List<VaccinationModel>) =
        vaccinations.last().doseNumber < vaccinations.last().totalSeriesOfDoses

    private fun getVaccineStartDayNotComplete(vaccineType: String): Long {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getVaccineEndDayNotComplete(vaccineType: String): Long {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getVaccineStartDayComplete(vaccineType: String): Long {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: run {
                0L
            }
    }

    private fun getVaccineEndDayComplete(vaccineType: String): Long {
        return validationRules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: run {
                0L
            }
    }
}