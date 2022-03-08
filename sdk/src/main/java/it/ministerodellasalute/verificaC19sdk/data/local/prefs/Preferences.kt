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
 *  Created by mykhailo.nester on 4/27/21 10:41 PM
 */

package it.ministerodellasalute.verificaC19sdk.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 *
 * This interface stores important values as [SharedPreferences]. This interface is implemented by
 * [PreferencesImpl] class.
 *
 */
interface Preferences {

    var resumeToken: Long

    var dateLastFetch: Long

    var drlDateLastFetch: Long

    var validationRulesJson: String?

    var sizeSingleChunkInByte: Long

    var fromVersion: Long

    var totalChunk: Long

    var chunk: Long

    var totalNumberUCVI: Long

    var totalSizeInByte: Long

    var currentVersion: Long

    var requestedVersion: Long

    var currentChunk: Long

    var authorizedToDownload: Long

    var authToResume: Long

    var isFrontCameraActive: Boolean

    var isTotemModeActive: Boolean

    var scanMode: String?

    var hasScanModeBeenChosen: Boolean

    var isSizeOverThreshold: Boolean

    var isDrlSyncActive: Boolean

    var shouldInitDownload: Boolean

    var maxRetryNumber: Int

    var isDoubleScanFlow: Boolean

    var userName: String?

    /**
     *
     * This method clears all values from the Shared Preferences file.
     */
    fun clear()

    fun clearDrlPrefs()

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener)
}

/**
 * This class implements the [Preferences] interface, defining its properties and method: the last
 * downloaded [resumeToken], the [dateLastFetch] executed, the [validationRulesJson] received, and
 * the [clear] method.
 */
class PreferencesImpl(context: Context) : Preferences {

    private var preferences: Lazy<SharedPreferences> = lazy {
        context.applicationContext.getSharedPreferences(PrefKeys.USER_PREF, Context.MODE_PRIVATE)
    }

    override var resumeToken by LongPreference(preferences, PrefKeys.KEY_RESUME_TOKEN, -1)

    override var dateLastFetch by LongPreference(preferences, PrefKeys.KEY_DATE_LAST_FETCH, -1)

    override var drlDateLastFetch by LongPreference(
        preferences,
        PrefKeys.KEY_DRL_DATE_LAST_FETCH,
        -1
    )

    override var validationRulesJson by StringPreference(
        preferences,
        PrefKeys.KEY_VALIDATION_RULES,
        ""
    )

    override var fromVersion by LongPreference(preferences, PrefKeys.KEY_FROM_VERSION, 0)

    override var totalSizeInByte by LongPreference(preferences, PrefKeys.KEY_TOTAL_BYTE_SIZE, 0)

    override var totalChunk by LongPreference(preferences, PrefKeys.KEY_TOTAL_CHUNK, 0)

    override var chunk by LongPreference(preferences, PrefKeys.KEY_CHUNK, 0)

    override var totalNumberUCVI by LongPreference(preferences, PrefKeys.KEY_TOTAL_NUMBER_UCVI, 0)

    override var sizeSingleChunkInByte by LongPreference(
        preferences,
        PrefKeys.KEY_SIZE_SINGLE_CHUNK_IN_BYTE,
        0
    )

    override var currentVersion by LongPreference(preferences, PrefKeys.CURRENT_VERSION, 0)

    override var requestedVersion by LongPreference(preferences, PrefKeys.REQUESTED_VERSION, 0)

    override var currentChunk by LongPreference(preferences, PrefKeys.CURRENT_CHUNK, 0)

    override var authorizedToDownload by LongPreference(
        preferences,
        PrefKeys.AUTHORIZED_TO_DOWNLOAD,
        1
    )

    override var authToResume by LongPreference(preferences, PrefKeys.AUTH_TO_RESUME, -1L)
    override var isFrontCameraActive by BooleanPreference(
        preferences,
        PrefKeys.KEY_FRONT_CAMERA_ACTIVE,
        false
    )

    override var isTotemModeActive by BooleanPreference(
        preferences,
        PrefKeys.KEY_TOTEM_MODE_ACTIVE,
        false
    )

    override var isSizeOverThreshold by BooleanPreference(
        preferences,
        PrefKeys.KEY_SIZE_OVER_THRESHOLD,
        false
    )

    override var isDrlSyncActive by BooleanPreference(
        preferences,
        PrefKeys.KEY_IS_DRL_SYNC_ACTIVE,
        true
    )

    override var shouldInitDownload by BooleanPreference(
        preferences,
        PrefKeys.KEY_SHOULD_INIT_DOWNLOAD,
        false
    )

    override var maxRetryNumber by IntPreference(preferences, PrefKeys.KEY_MAX_RETRY_NUM, 1)

    override var scanMode by StringPreference(preferences, PrefKeys.KEY_SCAN_MODE, "3G")

    override var hasScanModeBeenChosen by BooleanPreference(
        preferences,
        PrefKeys.KEY_SCAN_MODE_FLAG,
        false
    )

    override var isDoubleScanFlow by BooleanPreference(
        preferences,
        PrefKeys.KEY_IS_DOUBLE_SCAN_FLOW,
        false
    )
    override var userName by StringPreference(preferences, PrefKeys.KEY_USER_NAME, "")

    override fun clear() {
        preferences.value.edit().clear().apply()
    }

    override fun clearDrlPrefs() {
        preferences.value.edit().remove(PrefKeys.KEY_DRL_DATE_LAST_FETCH).apply()
        preferences.value.edit().remove(PrefKeys.KEY_FROM_VERSION).apply()
        preferences.value.edit().remove(PrefKeys.KEY_FROM_VERSION).apply()
        preferences.value.edit().remove(PrefKeys.KEY_TOTAL_CHUNK).apply()
        preferences.value.edit().remove(PrefKeys.KEY_CHUNK).apply()
        preferences.value.edit().remove(PrefKeys.KEY_TOTAL_NUMBER_UCVI).apply()
        preferences.value.edit().remove(PrefKeys.KEY_SIZE_SINGLE_CHUNK_IN_BYTE).apply()
        preferences.value.edit().remove(PrefKeys.NUM_DI_ADD).apply()
        preferences.value.edit().remove(PrefKeys.NUM_DI_DELETE).apply()
        preferences.value.edit().remove(PrefKeys.CURRENT_VERSION).apply()
        preferences.value.edit().remove(PrefKeys.REQUESTED_VERSION).apply()
        preferences.value.edit().remove(PrefKeys.CURRENT_CHUNK).apply()
        preferences.value.edit().remove(PrefKeys.AUTHORIZED_TO_DOWNLOAD).apply()
        preferences.value.edit().remove(PrefKeys.AUTH_TO_RESUME).apply()
        preferences.value.edit().remove(PrefKeys.KEY_SIZE_OVER_THRESHOLD).apply()
        preferences.value.edit().remove(PrefKeys.KEY_TOTAL_BYTE_SIZE).apply()
        preferences.value.edit().remove(PrefKeys.KEY_IS_DRL_SYNC_ACTIVE).apply()
        preferences.value.edit().remove(PrefKeys.KEY_SHOULD_INIT_DOWNLOAD).apply()
        preferences.value.edit().remove(PrefKeys.KEY_MAX_RETRY_NUM).apply()
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.value.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.value.unregisterOnSharedPreferenceChangeListener(listener)
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

class BooleanPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.value.getBoolean(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.value.edit { putBoolean(name, value) }
    }
}

class IntPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: Int
) : ReadWriteProperty<Any, Int> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Int {
        return preferences.value.getInt(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        preferences.value.edit { putInt(name, value) }
    }
}
