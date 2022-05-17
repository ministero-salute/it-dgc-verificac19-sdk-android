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
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Setting
import it.ministerodellasalute.verificaC19sdk.model.Country
import it.ministerodellasalute.verificaC19sdk.model.MedicinalProduct
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum
import it.ministerodellasalute.verificaC19sdk.util.ConversionUtility

class Settings(rulesJson: String?) {

    companion object {
        const val NO_VALUE = 0L
    }

    private val settings: Array<Setting> = Gson().fromJson(rulesJson, Array<Setting>::class.java)


    fun getVaccineStartDayNotComplete(vaccineType: String): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getVaccineEndDayNotComplete(vaccineType: String): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineStartDayComplete(vaccineType: String): Long {
        return settings
            .find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineEndDayComplete(vaccineType: String): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineEndDayCompleteUnder18(): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_UNDER_18.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getVaccineCompleteUnder18Offset(): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_COMPLETE_UNDER_18_OFFSET.value }?.value?.toLong()
            ?: NO_VALUE


    }

    fun getMolecularTestStartHour(): Long {
        return settings.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_START_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getMolecularTestEndHour(): Long {
        return settings.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_END_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getRapidTestStartHour(): Long {
        return settings.find { it.name == ValidationRulesEnum.RAPID_TEST_START_HOUR.value }?.value?.toLong()
            ?: NO_VALUE

    }

    fun getRapidTestEndHour(): Long {
        return settings.find { it.name == ValidationRulesEnum.RAPID_TEST_END_HOUR.value }?.value?.toLong()
            ?: NO_VALUE
    }


    fun getRecoveryCertStartDay(): Long {
        return settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertPVStartDay(): Long {
        return settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertEndDay(): Long {
        return settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getRecoveryCertPvEndDay(): Long {
        return settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun getAppMinVersion(): String {
        return settings.find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value }?.value.orEmpty()

    }

    fun getSDKMinVersion(): String {
        return settings.find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value }?.value.orEmpty()
    }

    fun getVaccineStartDayCompleteUnified(countryCode: String, medicinalProduct: String): Long {
        val daysToAdd = if (medicinalProduct == MedicinalProduct.JANSEN) getVaccineStartDayComplete(MedicinalProduct.JANSEN) else NO_VALUE

        val startDay = when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
        return startDay + daysToAdd
    }

    fun getVaccineEndDayCompleteUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineStartDayBoosterUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_BOOSTER_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineEndDayBoosterUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_BOOSTER_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getRecoveryCertStartDayUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getRecoveryCertEndDayUnified(countryCode: String): Long {
        return when (countryCode) {
            Country.IT.value -> settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY_IT.value }?.value?.toLong()
                ?: NO_VALUE
            else -> settings.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY_NOT_IT.value }?.value?.toLong()
                ?: NO_VALUE

        }
    }

    fun getVaccineEndDayCompleteExtendedEMA(): Long {
        return settings.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE_EXTENDED_EMA.value }?.value?.toLong()
            ?: NO_VALUE
    }

    fun isEMA(medicinalProduct: String, countryOfVaccination: String): Boolean {
        val isStandardEma = settings.find { it.name == ValidationRulesEnum.EMA_VACCINES.value }?.value?.split(";")?.contains(medicinalProduct) ?: false
        val isSpecialEma = medicinalProduct == MedicinalProduct.SPUTNIK && countryOfVaccination == Country.SM.value
        return isStandardEma || isSpecialEma
    }

    fun getBaseScanModeDescription(): String {
        return settings.find { it.name == ValidationRulesEnum.BASE_SCAN_MODE_DESCRIPTION.value }?.value.orEmpty()
    }

    fun getReinforcedScanModeDescription(): String {
        return settings.find { it.name == ValidationRulesEnum.REINFORCED_SCAN_MODE_DESCRIPTION.value }?.value.orEmpty()
    }

    fun getBoosterScanModeDescription(): String {
        return settings.find { it.name == ValidationRulesEnum.BOOSTER_SCAN_MODE_DESCRIPTION.value }?.value.orEmpty()
    }

    fun getItalyEntryScanModeDescription(): String {
        return settings.find { it.name == ValidationRulesEnum.ITALY_ENTRY_SCAN_MODE_DESCRIPTION.value }?.value.orEmpty()
    }

    fun getInfoScanModePopup(): String {
        return settings.find { it.name == ValidationRulesEnum.INFO_SCAN_MODE_POPUP.value }?.value.orEmpty()
    }

    fun getErrorScanModePopup(): String? {
        return settings.find { it.name == ValidationRulesEnum.ERROR_SCAN_MODE_POPUP.value }?.value
    }

    fun getValidFaqText(): String {
        return settings.find { it.name == ValidationRulesEnum.VALID_FAQ_TEXT.value }?.value.orEmpty()
    }

    fun getValidFaqLink(): String {
        return settings.find { it.name == ValidationRulesEnum.VALID_FAQ_LINK.value }?.value.orEmpty()
    }

    fun getNotValidFaqText(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_VALID_FAQ_TEXT.value }?.value.orEmpty()
    }

    fun getNotValidFaqLink(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_VALID_FAQ_LINK.value }?.value.orEmpty()
    }

    fun getVerificationNeededFaqText(): String {
        return settings.find { it.name == ValidationRulesEnum.VERIFICATION_NEEDED_FAQ_TEXT.value }?.value.orEmpty()
    }

    fun getVerificationNeededFaqLink(): String {
        return settings.find { it.name == ValidationRulesEnum.VERIFICATION_NEEDED_FAQ_LINK.value }?.value.orEmpty()
    }

    fun getNotValidYetFaqText(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_VALID_YET_FAQ_TEXT.value }?.value.orEmpty()
    }

    fun getNotValidYetFaqLink(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_VALID_YET_FAQ_LINK.value }?.value.orEmpty()
    }

    fun getNotEuDgcFaqText(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_EU_DGC_FAQ_TEXT.value }?.value.orEmpty()
    }

    fun getNotEuDgcFaqLink(): String {
        return settings.find { it.name == ValidationRulesEnum.NOT_EU_DGC_FAQ_LINK.value }?.value.orEmpty()
    }

    fun isDrlSyncActive(): Boolean {
        return settings.find { it.name == ValidationRulesEnum.DRL_SYNC_ACTIVE.name }
            ?.let { ConversionUtility.stringToBoolean(it.value) } ?: true

    }

    fun isDrlSyncActiveEU(): Boolean {
        return settings.find { it.name == ValidationRulesEnum.DRL_SYNC_ACTIVE_EU.name }
            ?.let { ConversionUtility.stringToBoolean(it.value) } ?: true
    }

    fun getMaxRetryNumber(): Int {
        return settings.find { it.name == ValidationRulesEnum.MAX_RETRY.name }?.value?.toInt() ?: 1
    }

    fun getBlackList(): List<String> {
        return settings.find { it.name == ValidationRulesEnum.BLACK_LIST_UVCI.value }?.value?.trim()?.split(";")
            ?: run {
                emptyList()
            }
    }

}