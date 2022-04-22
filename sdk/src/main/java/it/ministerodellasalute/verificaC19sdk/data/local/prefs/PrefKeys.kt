/*
 *  ---license-start
 *  eu-digital-green-certificates / dgca-verifier-app-android
 *  ---
 *  Copyright (C) 2021 T-Systems International GmbH and all other contributors
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
 */

package it.ministerodellasalute.verificaC19sdk.data.local.prefs

object PrefKeys {
    const val KEY_DRL_STATE_IT = "drl_state_it"
    const val KEY_DRL_STATE_EU = "drl_state_eu"

    const val USER_PREF = "dgca.verifier.app.pref"
    const val KEY_RESUME_TOKEN = "resume_token"
    const val KEY_DATE_LAST_FETCH = "date_last_fetch"
    const val KEY_VALIDATION_RULES = "validation_rules"

    const val AUTHORIZED_TO_DOWNLOAD = "authorized_to_download"
    const val AUTH_TO_RESUME = "auth_to_resume"


    const val KEY_FRONT_CAMERA_ACTIVE = "front_camera_active"
    const val KEY_TOTEM_MODE_ACTIVE = "totem_mode_active"

    const val KEY_IS_DRL_SYNC_ACTIVE = "is_drl_sync_active"
    const val KEY_SHOULD_INIT_DOWNLOAD = "should_init_download"
    const val KEY_MAX_RETRY_NUM = "max_retry_num"

    const val KEY_SCAN_MODE = "scan_mode"
    const val KEY_SCAN_MODE_FLAG = "scan_mode_flag"

    const val KEY_IS_DOUBLE_SCAN_FLOW = "double_scan_flow"
    const val KEY_USER_NAME = "user_name"
}