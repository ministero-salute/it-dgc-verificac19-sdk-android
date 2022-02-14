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

    const val USER_PREF = "dgca.verifier.app.pref"
    const val KEY_RESUME_TOKEN = "resume_token"
    const val KEY_DATE_LAST_FETCH = "date_last_fetch"
    const val KEY_DRL_DATE_LAST_FETCH = "drl_date_last_fetch"
    const val KEY_VALIDATION_RULES = "validation_rules"

    const val KEY_FROM_VERSION = "from_version"
    const val KEY_TOTAL_CHUNK = "total_chunk"
    const val KEY_CHUNK = "chunk"
    const val KEY_TOTAL_NUMBER_UCVI = "total_number_ucvi"

    const val KEY_SIZE_SINGLE_CHUNK_IN_BYTE = "size_single_chunk_in_byte"
    const val NUM_DI_ADD = "num_di_add"
    const val NUM_DI_DELETE = "num_di_delete"
    const val CURRENT_VERSION = "current_version"
    const val REQUESTED_VERSION = "requested_version"
    const val CURRENT_CHUNK = "current_chunk"
    const val AUTHORIZED_TO_DOWNLOAD = "authorized_to_download"
    const val AUTH_TO_RESUME = "auth_to_resume"


    const val KEY_FRONT_CAMERA_ACTIVE = "front_camera_active"
    const val KEY_TOTEM_MODE_ACTIVE = "totem_mode_active"

    const val KEY_SIZE_OVER_THRESHOLD = "size_over_thresold"
    const val KEY_TOTAL_BYTE_SIZE = "total_byte_size"
    const val KEY_IS_DRL_SYNC_ACTIVE = "is_drl_sync_active"
    const val KEY_SHOULD_INIT_DOWNLOAD = "should_init_download"
    const val KEY_MAX_RETRY_NUM = "max_retry_num"

    const val KEY_SCAN_MODE = "scan_mode"
    const val KEY_SCAN_MODE_FLAG = "scan_mode_flag"
}