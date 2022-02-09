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

    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate
    private var extendedDate: LocalDate? = null

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
        val countryCode =
            if (scanMode == ScanMode.STANDARD) vaccination.countryOfVaccination else Country.IT.value

        if (!ruleSet.hasRulesForVaccine(vaccineType)) return CertificateStatus.NOT_VALID
        if (vaccination.isNotAllowed()) return CertificateStatus.NOT_VALID

        return try {
            startDate = retrieveStartDate(vaccination, ruleSet, countryCode)
            endDate = retrieveEndDate(vaccination, ruleSet, countryCode, scanMode)
            extendedDate = retrieveExtendedDate(vaccination, ruleSet)

            Log.d("VaccineDates", "Start: $startDate End: $endDate")
            validateWithScanMode(certificateModel, ruleSet)
        } catch (e: Exception) {
            CertificateStatus.NOT_EU_DCC
        }
    }

    private fun validateWithScanMode(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        return when (certificateModel.scanMode) {
            ScanMode.STANDARD -> vaccineStandardStrategy(certificateModel, ruleSet)
            ScanMode.STRENGTHENED -> vaccineStrengthenedStrategy(certificateModel, ruleSet)
            ScanMode.BOOSTER -> vaccineBoosterStrategy(certificateModel)
            ScanMode.SCHOOL -> vaccineSchoolStrategy(certificateModel)
            ScanMode.WORK -> vaccineWorkStrategy(certificateModel)
            ScanMode.ENTRY_ITALY -> vaccineEntryItalyStrategy(certificateModel, ruleSet)
            else -> {CertificateStatus.NOT_EU_DCC}
        }
    }

    private fun vaccineStandardStrategy(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        if (!MedicinalProduct.isEMA(vaccination.medicinalProduct)) return CertificateStatus.NOT_VALID

        val countryCode =
            if (vaccination.countryOfVaccination != Country.IT.value && vaccination.isComplete() && !vaccination.isBooster())
                Country.IT.value
            else
                vaccination.countryOfVaccination

        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()
        startDate =
            when {
                vaccination.isComplete() -> {
                    val startDaysToAdd =
                        if (vaccination.isBooster()) ruleSet.getVaccineStartDayBoosterUnified(countryCode)
                        else ruleSet.getVaccineStartDayCompleteUnified(countryCode, vaccination.medicinalProduct)
                    dateOfVaccination.plusDays(startDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineStartDayNotComplete(vaccination.medicinalProduct))
                else -> dateOfVaccination
            }
        endDate =
            when {
                vaccination.isComplete() -> {
                    val endDaysToAdd =
                        when {
                            vaccination.isBooster() -> ruleSet.getVaccineEndDayBoosterUnified(countryCode)
                            else -> ruleSet.getVaccineEndDayCompleteUnified(countryCode)
                        }
                    dateOfVaccination.plusDays(endDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(ruleSet.getVaccineEndDayNotComplete(vaccination.medicinalProduct))
                else -> dateOfVaccination
            }

        when {
            LocalDate.now().isBefore(startDate) -> return CertificateStatus.NOT_VALID_YET
            LocalDate.now().isAfter(endDate) -> return CertificateStatus.NOT_VALID
        }
        return when {
            vaccination.isComplete() -> CertificateStatus.VALID
            vaccination.isNotComplete() -> CertificateStatus.NOT_VALID
            else -> CertificateStatus.NOT_EU_DCC
        }
    }

    private fun vaccineStrengthenedStrategy(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        val country = vaccination.countryOfVaccination
        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()

        when (country) {
            Country.IT.value -> {
                vaccineStandardStrategy(certificateModel, ruleSet)
            }
            else -> {
                when {
                    vaccination.isNotComplete() -> {
                        if (MedicinalProduct.isEMA(vaccination.medicinalProduct)) {
                            startDate = dateOfVaccination.plusDays(ruleSet.getVaccineStartDayNotComplete(vaccination.medicinalProduct))
                            endDate = dateOfVaccination.plusDays(ruleSet.getVaccineEndDayNotComplete(vaccination.medicinalProduct))
                        } else {
                            startDate = dateOfVaccination.plusDays(ruleSet.getVaccineStartDayNotCompleteNotEMA())
                            endDate = dateOfVaccination.plusDays(ruleSet.getVaccineEndDayNotCompleteNotEMA())
                        }
                    }
                    vaccination.isComplete() -> {
                        val startDaysToAdd =
                            if (vaccination.isBooster()) ruleSet.getVaccineStartDayBoosterUnified(Country.IT.value)
                            else ruleSet.getVaccineStartDayCompleteUnified(Country.IT.value, vaccination.medicinalProduct)

                        val endDaysToAdd =
                            if (vaccination.isBooster()) ruleSet.getVaccineEndDayBoosterUnified(Country.IT.value)
                            else ruleSet.getVaccineEndDayCompleteUnified(Country.IT.value)

                        startDate = dateOfVaccination.plusDays(startDaysToAdd)
                        endDate = dateOfVaccination.plusDays(endDaysToAdd)
                    }
                }
            }
        }
        when {
            vaccination.isNotComplete() || vaccination.isBooster() -> {
                when {
                    LocalDate.now().isBefore(startDate) -> return CertificateStatus.NOT_VALID_YET
                    LocalDate.now().isAfter(endDate) -> return CertificateStatus.NOT_VALID
                }
            }
            else -> {
                when {
                    MedicinalProduct.isEMA(vaccination.medicinalProduct) -> {
                        return when {
                            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                            LocalDate.now().isBefore(endDate) -> CertificateStatus.VALID
                            LocalDate.now().isBefore(extendedDate) -> CertificateStatus.TEST_NEEDED
                            else -> CertificateStatus.NOT_VALID

                        }
                    }
                    else -> {
                        return when {
                            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                            LocalDate.now().isBefore(extendedDate) -> CertificateStatus.TEST_NEEDED
                            else -> CertificateStatus.NOT_VALID
                        }
                    }
                }
            }
        }
        return CertificateStatus.NOT_EU_DCC
    }

    private fun vaccineBoosterStrategy(certificateModel: CertificateModel): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        return when {
            vaccination.isComplete() -> {
                if (vaccination.isBooster()) return CertificateStatus.VALID
                return CertificateStatus.TEST_NEEDED
            }
            vaccination.isNotComplete() -> CertificateStatus.NOT_VALID
            else -> return CertificateStatus.NOT_EU_DCC
        }
    }

    private fun vaccineSchoolStrategy(certificateModel: CertificateModel): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        return when {
            vaccination.isComplete() -> CertificateStatus.VALID
            vaccination.isNotComplete() -> CertificateStatus.NOT_VALID
            else -> return CertificateStatus.NOT_EU_DCC
        }
    }

    private fun vaccineWorkStrategy(certificateModel: CertificateModel): CertificateStatus {
        TODO("Not yet implemented")
    }

    private fun vaccineEntryItalyStrategy(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        if (!MedicinalProduct.isEMA(vaccination.medicinalProduct)) return CertificateStatus.NOT_VALID

        return when {
            vaccination.isNotComplete() -> CertificateStatus.NOT_VALID
            vaccination.isComplete() -> CertificateStatus.VALID
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
    ): LocalDate {
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
                else -> dateOfVaccination
            }
        }
    }

    private fun retrieveExtendedDate(
        vaccination: VaccinationModel,
        ruleSet: RuleSet
    ): LocalDate? {
        vaccination.run {
            val dateOfVaccination = dateOfVaccination.toLocalDate()
            val extendedDaysToAdd =
                ruleSet.getVaccineEndDayCompleteExtendedEMA()

            return dateOfVaccination.plusDays(extendedDaysToAdd)
        }
    }
}