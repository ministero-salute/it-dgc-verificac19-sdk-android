/*
 *  license-start
 *  
 *  Copyright (C) 2021 Ministero della Salute and all other contributors
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/


package it.ministerodellasalute.verificaC19sdk.model


/**
 *
 * This class represents the various fields of the validation rules JSON.
 *
 */
enum class ValidationRulesEnum(val value: String) {
    APP_MIN_VERSION("android"),
    SDK_MIN_VERSION("sdk"),
    RECOVERY_CERT_START_DAY("recovery_cert_start_day"),
    RECOVERY_CERT_PV_START_DAY("recovery_pv_cert_start_day"),
    RECOVERY_CERT_END_DAY("recovery_cert_end_day"),
    RECOVERY_CERT_PV_END_DAY("recovery_pv_cert_end_day"),
    MOLECULAR_TEST_START_HOUR("molecular_test_start_hours"),
    MOLECULAR_TEST_END_HOUR("molecular_test_end_hours"),
    RAPID_TEST_START_HOUR("rapid_test_start_hours"),
    RAPID_TEST_END_HOUR("rapid_test_end_hours"),
    VACCINE_START_DAY_NOT_COMPLETE("vaccine_start_day_not_complete"),
    VACCINE_END_DAY_NOT_COMPLETE("vaccine_end_day_not_complete"),
    VACCINE_START_DAY_COMPLETE("vaccine_start_day_complete"),
    VACCINE_END_DAY_COMPLETE("vaccine_end_day_complete"),

    VACCINE_START_DAY_COMPLETE_IT("vaccine_start_day_complete_IT"),
    VACCINE_END_DAY_COMPLETE_IT("vaccine_end_day_complete_IT"),
    VACCINE_START_DAY_BOOSTER_IT("vaccine_start_day_booster_IT"),
    VACCINE_END_DAY_BOOSTER_IT("vaccine_end_day_booster_IT"),

    VACCINE_START_DAY_COMPLETE_NOT_IT("vaccine_start_day_complete_NOT_IT"),
    VACCINE_END_DAY_COMPLETE_NOT_IT("vaccine_end_day_complete_NOT_IT"),
    VACCINE_START_DAY_BOOSTER_NOT_IT("vaccine_start_day_booster_NOT_IT"),
    VACCINE_END_DAY_BOOSTER_NOT_IT("vaccine_end_day_booster_NOT_IT"),

    RECOVERY_CERT_START_DAY_IT("recovery_cert_start_day_IT"),
    RECOVERY_CERT_END_DAY_IT("recovery_cert_end_day_IT"),
    RECOVERY_CERT_START_DAY_NOT_IT("recovery_cert_start_day_NOT_IT"),
    RECOVERY_CERT_END_DAY_NOT_IT("recovery_cert_end_day_NOT_IT"),

    VACCINE_END_DAY_COMPLETE_EXTENDED_EMA("vaccine_end_day_complete_extended_EMA"),
    EMA_VACCINES("EMA_vaccines"),

    BLACK_LIST_UVCI("black_list_uvci"),
    DRL_SYNC_ACTIVE("DRL_SYNC_ACTIVE"),
    MAX_RETRY("MAX_RETRY"),

    BASE_SCAN_MODE_DESCRIPTION("3G_scan_mode_description"),
    REINFORCED_SCAN_MODE_DESCRIPTION("2G_scan_mode_description"),
    BOOSTER_SCAN_MODE_DESCRIPTION("booster_scan_mode_description"),
    ITALY_ENTRY_SCAN_MODE_DESCRIPTION("italy_entry_scan_mode_description"),
    WORK_SCAN_MODE_DESCRIPTION("work_scan_mode_description"),
    INFO_SCAN_MODE_POPUP("info_scan_mode_popup"),
    ERROR_SCAN_MODE_POPUP("error_scan_mode_popup"),
    VALID_FAQ_TEXT("valid_faq_text"),
    VALID_FAQ_LINK("valid_faq_link"),
    NOT_VALID_FAQ_TEXT("not_valid_faq_text"),
    NOT_VALID_FAQ_LINK("not_valid_faq_link"),
    VERIFICATION_NEEDED_FAQ_TEXT("verification_needed_faq_text"),
    VERIFICATION_NEEDED_FAQ_LINK("verification_needed_faq_link"),
    NOT_VALID_YET_FAQ_TEXT("not_valid_yet_faq_text"),
    NOT_VALID_YET_FAQ_LINK("not_valid_yet_faq_link"),
    NOT_EU_DGC_FAQ_TEXT("not_eu_dgc_faq_text"),
    NOT_EU_DGC_FAQ_LINK("not_eu_dgc_faq_link")
}