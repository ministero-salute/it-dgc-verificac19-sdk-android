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
 *  Created by kaizen-7 on 25/01/22, 14:42
 */

package it.ministerodellasalute.verificaC19sdk.model.validation

import com.google.gson.Gson
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.Country
import it.ministerodellasalute.verificaC19sdk.model.MedicinalProduct
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum

class RuleSet(rulesJson: String?) {

    companion object {
        const val NO_VALUE = 0L
    }

    private val rules: Array<Rule> = Gson().fromJson(rulesJson, Array<Rule>::class.java)


    fun getVaccineStartDayNotComplete(vaccineType: String): Long {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getVaccineEndDayNotComplete(vaccineType: String): Long {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineStartDayComplete(vaccineType: String): Long {
        return rules
            .find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineEndDayComplete(vaccineType: String): Long {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getMolecularTestStartHour(): Long {
        return rules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_START_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getMolecularTestEndHour(): Long {
        return rules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_END_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getRapidTestStartHour(): Long {
        return rules.find { it.name == ValidationRulesEnum.RAPID_TEST_START_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getRapidTestEndHour(): Long {
        return rules.find { it.name == ValidationRulesEnum.RAPID_TEST_END_HOUR.value }?.value?.toLong()
            ?: NO_VALUE
    }


    fun getRecoveryCertStartDay(): Long {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertPVStartDay(): Long {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertEndDay(): Long {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertPvEndDay(): Long {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getAppMinVersion(): String {
        return rules.find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value }?.value ?: ""

    }

    fun getSDKMinVersion(): String {
        return rules.find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value }?.value ?: ""
    }

    fun getVaccineStartDayCompleteUnified(countryCode: String, medicinalProduct: String): Long {
        val daysToAdd = if (medicinalProduct == MedicinalProduct.JANSEN) getVaccineStartDayComplete(MedicinalProduct.JANSEN) else NO_VALUE

        val startDay = when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
        return startDay + daysToAdd
    }

    fun getVaccineEndDayCompleteUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineStartDayBoosterUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineEndDayBoosterUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getRecoveryCertStartDayUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getRecoveryCertEndDayUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineEndDayCompleteExtendedEMA(): Long {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_EXTENDED_EMA.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun isEMA(medicinalProduct: String, countryOfVaccination: String): Boolean {
        val isStandardEma = rules.find { it.name == ValidationRulesEnum.EMA_VACCINES.value }?.value?.split(";")?.contains(medicinalProduct) ?: false
        val isSpecialEma = medicinalProduct == MedicinalProduct.SPUTNIK && countryOfVaccination == Country.SM.value
        return isStandardEma || isSpecialEma
    }

    fun getBaseScanModeDescription(): String? {
        return rules.find { it.name == ValidationRulesEnum.BASE_SCAN_MODE_DESCRIPTION.value }?.value
    }

    fun getReinforcedScanModeDescription(): String {
        return rules.find { it.name == ValidationRulesEnum.REINFORCED_SCAN_MODE_DESCRIPTION.value }?.value ?: run {
            ""
        }
    }

    fun getBoosterScanModeDescription(): String {
        return rules.find { it.name == ValidationRulesEnum.BOOSTER_SCAN_MODE_DESCRIPTION.value }?.value ?: run {
            ""
        }
    }

    fun getItalyEntryScanModeDescription(): String {
        return rules.find { it.name == ValidationRulesEnum.ITALY_ENTRY_SCAN_MODE_DESCRIPTION.value }?.value ?: run {
            ""
        }
    }

    fun getWorkScanModeDescription(): String {
        return rules.find { it.name == ValidationRulesEnum.WORK_SCAN_MODE_DESCRIPTION.value }?.value ?: run {
            ""
        }
    }

    fun getInfoScanModePopup(): String {
        return rules.find { it.name == ValidationRulesEnum.INFO_SCAN_MODE_POPUP.value }?.value ?: run {
            ""
        }
    }

    fun getErrorScanModePopup(): String? {
        return rules.find { it.name == ValidationRulesEnum.ERROR_SCAN_MODE_POPUP.value }?.value
    }

    fun getValidFaqText(): String {
        return rules.find { it.name == ValidationRulesEnum.VALID_FAQ_TEXT.value }?.value ?: run {
            ""
        }
    }

    fun getValidFaqLink(): String {
        return rules.find { it.name == ValidationRulesEnum.VALID_FAQ_LINK.value }?.value ?: run {
            ""
        }
    }

    fun getNotValidFaqText(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_VALID_FAQ_TEXT.value }?.value ?: run {
            ""
        }
    }

    fun getNotValidFaqLink(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_VALID_FAQ_LINK.value }?.value ?: run {
            ""
        }
    }

    fun getVerificationNeededFaqText(): String {
        return rules.find { it.name == ValidationRulesEnum.VERIFICATION_NEEDED_FAQ_TEXT.value }?.value ?: run {
            ""
        }
    }

    fun getVerificationNeededFaqLink(): String {
        return rules.find { it.name == ValidationRulesEnum.VERIFICATION_NEEDED_FAQ_LINK.value }?.value ?: run {
            ""
        }
    }

    fun getNotValidYetFaqText(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_VALID_YET_FAQ_TEXT.value }?.value ?: run {
            ""
        }
    }

    fun getNotValidYetFaqLink(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_VALID_YET_FAQ_LINK.value }?.value ?: run {
            ""
        }
    }

    fun getNotEuDgcFaqText(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_EU_DGC_FAQ_TEXT.value }?.value ?: run {
            ""
        }
    }

    fun getNotEuDgcFaqLink(): String {
        return rules.find { it.name == ValidationRulesEnum.NOT_EU_DGC_FAQ_LINK.value }?.value ?: run {
            ""
        }
    }

}