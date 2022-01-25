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

        if (!ruleSet.hasSettingsForVaccine(vaccineType)) return CertificateStatus.NOT_VALID
        if (vaccination.isSputnikNotFromSanMarino()) return CertificateStatus.NOT_VALID

        try {
            val dateOfVaccination = vaccination.dateOfVaccination
            val startDate: LocalDate
            val endDate: LocalDate
            when {
                vaccination.isComplete() -> {
                    val startDayNotComplete = ruleSet.getVaccineStartDayNotComplete(vaccineType)
                    val endDayNotComplete = ruleSet.getVaccineEndDayNotComplete(vaccineType)
                    startDate = dateOfVaccination.toLocalDate().plusDays(startDayNotComplete)
                    endDate = dateOfVaccination.toLocalDate().plusDays(endDayNotComplete)
                    Log.d("VaccineNotCompleteDates", "Start: $startDate End: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> if (ScanMode.BOOSTER == scanMode) CertificateStatus.NOT_VALID else CertificateStatus.VALID
                    }
                }
                vaccination.isNotComplete() -> {
                    val endDayComplete = ruleSet.getVaccineEndDayComplete(vaccineType)
                    if (isJohnsonVaccineComplete(vaccineType, vaccination)) {
                        startDate = dateOfVaccination.toLocalDate()
                        endDate = dateOfVaccination.toLocalDate().plusDays(endDayComplete)
                    } else {
                        startDate = dateOfVaccination.toLocalDate().plusDays(ruleSet.getVaccineStartDayComplete(vaccineType))
                        endDate = dateOfVaccination.toLocalDate().plusDays(endDayComplete)
                    }

                    Log.d("VaccineCompleteDates", "Start:$startDate End: $endDate")
                    return when {
                        startDate.isAfter(LocalDate.now()) -> CertificateStatus.NOT_VALID_YET
                        LocalDate.now().isAfter(endDate) -> CertificateStatus.NOT_VALID
                        else -> {
                            when (scanMode) {
                                ScanMode.BOOSTER -> {
                                    if (isJohnsonVaccineNotBooster(vaccineType, vaccination)) return CertificateStatus.TEST_NEEDED
                                    else if (vaccination.isNotBooster()) return CertificateStatus.TEST_NEEDED
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

    private fun isJohnsonVaccineNotBooster(
        vaccineType: String,
        vaccination: VaccinationModel
    ) =
        vaccineType == MedicinalProduct.JOHNSON && vaccination.doseNumber == vaccination.totalSeriesOfDoses && vaccination.doseNumber < 2

    private fun isJohnsonVaccineComplete(
        vaccineType: String,
        vaccination: VaccinationModel
    ) = vaccineType == MedicinalProduct.JOHNSON && (vaccination.doseNumber > vaccination.totalSeriesOfDoses) ||
            (vaccination.doseNumber == vaccination.totalSeriesOfDoses && vaccination.doseNumber >= 2)

}