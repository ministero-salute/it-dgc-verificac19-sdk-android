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
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.getAge
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toValidDateOfBirth
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
    override fun checkCertificate(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val vaccine = certificateModel.vaccinations?.last()!!
        if (vaccine.isNotComplete() && !settings.isEMA(vaccine.medicinalProduct, vaccine.countryOfVaccination)) return CertificateStatus.NOT_VALID
        return try {
            validateWithScanMode(certificateModel, settings)
        } catch (e: Exception) {
            CertificateStatus.NOT_EU_DCC
        }
    }

    private fun validateWithScanMode(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        return when (certificateModel.scanMode) {
            ScanMode.STANDARD -> vaccineStandardStrategy(certificateModel, settings)
            ScanMode.STRENGTHENED -> vaccineStrengthenedStrategy(certificateModel, settings)
            ScanMode.BOOSTER -> vaccineBoosterStrategy(certificateModel, settings)
            ScanMode.ENTRY_ITALY -> vaccineEntryItalyStrategy(certificateModel, settings)
            else -> {
                CertificateStatus.NOT_EU_DCC
            }
        }
    }

    private fun vaccineStandardStrategy(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!

        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()
        startDate =
            when {
                vaccination.isComplete() -> {
                    val startDaysToAdd =
                        if (vaccination.isBooster()) settings.getVaccineStartDayBoosterUnified(Country.IT.value)
                        else settings.getVaccineStartDayCompleteUnified(Country.IT.value, vaccination.medicinalProduct)
                    dateOfVaccination.plusDays(startDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(settings.getVaccineStartDayNotComplete(vaccination.medicinalProduct))
                else -> dateOfVaccination
            }
        endDate =
            when {
                vaccination.isComplete() -> {
                    val endDaysToAdd =
                        when {
                            vaccination.isBooster() -> settings.getVaccineEndDayBoosterUnified(Country.IT.value)
                            else -> settings.getVaccineEndDayCompleteUnified(Country.IT.value)
                        }
                    dateOfVaccination.plusDays(endDaysToAdd)
                }
                vaccination.isNotComplete() -> dateOfVaccination.plusDays(settings.getVaccineEndDayNotComplete(vaccination.medicinalProduct))
                else -> dateOfVaccination
            }

        return when {
            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
            LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
            !settings.isEMA(vaccination.medicinalProduct, vaccination.countryOfVaccination) -> CertificateStatus.NOT_VALID
            else -> CertificateStatus.VALID
        }
    }

    private fun vaccineStrengthenedStrategy(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        val country = vaccination.countryOfVaccination
        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()

        when (country) {
            Country.IT.value -> {
                return vaccineStandardStrategy(certificateModel, settings)
            }
            else -> {
                when {
                    vaccination.isNotComplete() -> {
                        if (settings.isEMA(vaccination.medicinalProduct, vaccination.countryOfVaccination)) {
                            startDate = dateOfVaccination.plusDays(settings.getVaccineStartDayNotComplete(vaccination.medicinalProduct))
                            endDate = dateOfVaccination.plusDays(settings.getVaccineEndDayNotComplete(vaccination.medicinalProduct))
                        } else {
                            return CertificateStatus.NOT_VALID
                        }
                    }
                    vaccination.isComplete() -> {
                        val startDaysToAdd =
                            if (vaccination.isBooster()) settings.getVaccineStartDayBoosterUnified(Country.IT.value)
                            else settings.getVaccineStartDayCompleteUnified(Country.IT.value, vaccination.medicinalProduct)

                        val endDaysToAdd =
                            if (vaccination.isBooster()) settings.getVaccineEndDayBoosterUnified(Country.IT.value)
                            else settings.getVaccineEndDayCompleteUnified(Country.IT.value)

                        val extendedDaysToAdd = settings.getVaccineEndDayCompleteExtendedEMA()

                        startDate = dateOfVaccination.plusDays(startDaysToAdd)
                        endDate = dateOfVaccination.plusDays(endDaysToAdd)
                        extendedDate = dateOfVaccination.plusDays(extendedDaysToAdd)
                    }
                }
            }
        }
        when {
            vaccination.isNotComplete() -> {
                return when {
                    !settings.isEMA(vaccination.medicinalProduct, country) -> CertificateStatus.NOT_VALID
                    LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                    LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
                    else -> CertificateStatus.VALID
                }
            }
            vaccination.isBooster() -> {
                return when {
                    LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                    LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
                    else -> if (settings.isEMA(
                            vaccination.medicinalProduct,
                            vaccination.countryOfVaccination
                        )
                    ) CertificateStatus.VALID else CertificateStatus.TEST_NEEDED
                }
            }
            else -> {
                when {
                    settings.isEMA(vaccination.medicinalProduct, vaccination.countryOfVaccination) -> {
                        return when {
                            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                            LocalDate.now().isBefore(endDate) || !LocalDate.now().isAfter(endDate) -> CertificateStatus.VALID
                            LocalDate.now().isBefore(extendedDate) || !LocalDate.now().isAfter(extendedDate) -> CertificateStatus.TEST_NEEDED
                            else -> CertificateStatus.EXPIRED

                        }
                    }
                    else -> {
                        return when {
                            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
                            LocalDate.now().isBefore(extendedDate) || !LocalDate.now().isAfter(extendedDate) -> CertificateStatus.TEST_NEEDED
                            else -> CertificateStatus.EXPIRED
                        }
                    }
                }
            }
        }
    }

    private fun vaccineBoosterStrategy(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()

        val startDaysToAdd =
            when {
                vaccination.isBooster() -> settings.getVaccineStartDayBoosterUnified(Country.IT.value)
                vaccination.isNotComplete() -> settings.getVaccineStartDayNotComplete(vaccination.medicinalProduct)
                else -> settings.getVaccineStartDayCompleteUnified(Country.IT.value, vaccination.medicinalProduct)
            }
        val endDaysToAdd =
            when {
                vaccination.isBooster() -> settings.getVaccineEndDayBoosterUnified(Country.IT.value)
                vaccination.isNotComplete() -> settings.getVaccineEndDayNotComplete(vaccination.medicinalProduct)
                else -> settings.getVaccineEndDayCompleteUnified(Country.IT.value)
            }
        startDate = dateOfVaccination.plusDays(startDaysToAdd)
        endDate = dateOfVaccination.plusDays(endDaysToAdd)

        return when {
            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
            LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
            vaccination.isComplete() -> {
                if (vaccination.isBooster()) {
                    if (settings.isEMA(vaccination.medicinalProduct, vaccination.countryOfVaccination)) {
                        CertificateStatus.VALID
                    } else CertificateStatus.TEST_NEEDED
                } else CertificateStatus.TEST_NEEDED
            }
            else -> CertificateStatus.NOT_VALID
        }
    }

    private fun vaccineEntryItalyStrategy(certificateModel: CertificateModel, settings: Settings): CertificateStatus {
        val vaccination = certificateModel.vaccinations?.last()!!
        val dateOfVaccination = vaccination.dateOfVaccination.toLocalDate()
        val birthDate = (certificateModel.dateOfBirth?.toValidDateOfBirth())?.plusDays(settings.getVaccineCompleteUnder18Offset())
        val isUserUnderage = birthDate?.getAge()!! < Const.VACCINE_UNDERAGE_AGE

        val startDaysToAdd =
            when {
                vaccination.isBooster() -> settings.getVaccineStartDayBoosterUnified(Country.NOT_IT.value)
                vaccination.isNotComplete() -> settings.getVaccineStartDayNotComplete(vaccination.medicinalProduct)
                else -> settings.getVaccineStartDayCompleteUnified(Country.NOT_IT.value, vaccination.medicinalProduct)
            }

        val endDaysToAdd =
            when {
                vaccination.isComplete() && isUserUnderage -> settings.getVaccineEndDayCompleteUnder18()
                vaccination.isBooster() -> settings.getVaccineEndDayBoosterUnified(Country.NOT_IT.value)
                vaccination.isNotComplete() -> settings.getVaccineEndDayNotComplete(vaccination.medicinalProduct)
                else -> settings.getVaccineEndDayCompleteUnified(Country.NOT_IT.value)
            }

        startDate = dateOfVaccination.plusDays(startDaysToAdd)
        endDate = dateOfVaccination.plusDays(endDaysToAdd)

        return when {
            LocalDate.now().isBefore(startDate) -> CertificateStatus.NOT_VALID_YET
            LocalDate.now().isAfter(endDate) -> CertificateStatus.EXPIRED
            !settings.isEMA(vaccination.medicinalProduct, vaccination.countryOfVaccination) -> CertificateStatus.NOT_VALID
            vaccination.isNotComplete() -> CertificateStatus.NOT_VALID
            else -> CertificateStatus.VALID
        }
    }
}