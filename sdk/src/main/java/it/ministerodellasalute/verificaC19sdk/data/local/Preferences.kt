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
 *  Created by mykhailo.nester on 4/27/21 10:41 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Preferences {

    var resumeToken: Long

    var dateLastFetch: Long

    var validationRulesJson: String?

    var sizeSingleChunkInByte: Long

    //var fromVersion: Long

    var lastDownloadedVersion: Long

    var lastChunk: Long

    var lastDownloadedChunk: Long

    var numDiAdd: Long

    var numDiDelete: Long

    var currentVersion: Long

    var requestedVersion: Long

    var currentChunk: Long

    var authorizedToDownload: Long

    var blockCRLdownload: Long

    var authToResume: Long

    fun clear()
}

/**
 * [Preferences] impl backed by [android.content.SharedPreferences].
 */
class PreferencesImpl(context: Context) : Preferences {

    private var preferences: Lazy<SharedPreferences> = lazy {
        context.applicationContext.getSharedPreferences(USER_PREF, Context.MODE_PRIVATE)
    }

    override var resumeToken by LongPreference(preferences, KEY_RESUME_TOKEN, -1)

    override var dateLastFetch by LongPreference(preferences, KEY_DATE_LAST_FETCH, -1)

    override var validationRulesJson by StringPreference(preferences, KEY_VALIDATION_RULES, "")

    //override var fromVersion by LongPreference(preferences, KEY_FROM_VERSION,0)

    override var lastDownloadedVersion by LongPreference(preferences, KEY_LAST_DOWNLOADED_VERSION,0)

    override var lastChunk by LongPreference(preferences, KEY_LAST_CHUNK,0)

    override var lastDownloadedChunk by LongPreference(preferences, KEY_DOWNLOADED_LAST_CHUNK,0)

    override var sizeSingleChunkInByte  by LongPreference(preferences, KEY_SIZE_SINGLE_CHUNK_IN_BYTE,0)

    override var numDiAdd  by LongPreference(preferences, NUM_DI_ADD,0)

    override var numDiDelete  by LongPreference(preferences, NUM_DI_DELETE,0)

    override var currentVersion  by LongPreference(preferences, CURRENT_VERSION,0)

    override var requestedVersion  by LongPreference(preferences, REQUESTED_VERSION,0)

    override var currentChunk  by LongPreference(preferences, CURRENT_CHUNK,0)

    override var authorizedToDownload by LongPreference(preferences, AUTHORIZED_TO_DOWNLOAD,1)

    override var blockCRLdownload by LongPreference(preferences, BLOCK_CRL_DOWNLOAD,0)

    override var authToResume by LongPreference(preferences, AUTH_TO_RESUME,0)

    override fun clear() {
        preferences.value.edit().clear().apply()
    }

    companion object {
        private const val USER_PREF = "dgca.verifier.app.pref"
        private const val KEY_RESUME_TOKEN = "resume_token"
        private const val KEY_DATE_LAST_FETCH = "date_last_fetch"
        private const val KEY_VALIDATION_RULES = "validation_rules"


        //private const val KEY_FROM_VERSION = "from_version"
        private const val KEY_LAST_DOWNLOADED_VERSION = "key_last_downloaded_version"
        private const val KEY_LAST_CHUNK = "last_chunk"
        private const val KEY_DOWNLOADED_LAST_CHUNK = "last_downloed_chunk"
        private const val KEY_SIZE_SINGLE_CHUNK_IN_BYTE = "size_single_chunk_in_byte"
        private const val NUM_DI_ADD = "num_di_add"
        private const val NUM_DI_DELETE = "num_di_delete"
        private const val CURRENT_VERSION = "current_version"
        private const val REQUESTED_VERSION = "requested_version"
        private const val CURRENT_CHUNK = "current_chunk"
        private const val AUTHORIZED_TO_DOWNLOAD = "authorized_to_download"
        private const val BLOCK_CRL_DOWNLOAD = "block_crl_download"
        private const val AUTH_TO_RESUME = "auth_to_resume"


    }
}

class StringPreference(
        private val preferences: Lazy<SharedPreferences>,
        private val name: String,
        private val defaultValue: String
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.value.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.value.edit { putString(name, value) }
    }
}

class LongPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Long
) : ReadWriteProperty<Any, Long> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Long {
        return preferences.value.getLong(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
        preferences.value.edit { putLong(name, value) }
    }
}