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
import com.google.gson.Gson
import it.ministerodellasalute.verificaC19sdk.model.DrlFlowType
import it.ministerodellasalute.verificaC19sdk.model.DrlState
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

    var validationRulesJson: String?

    var authorizedToDownload: Long

    var authToResume: Long

    var isFrontCameraActive: Boolean

    var isTotemModeActive: Boolean

    var scanMode: String?

    var isDrlSyncActive: Boolean

    var isDrlSyncActiveEU: Boolean

    var shouldInitDownload: Boolean

    var maxRetryNumber: Int

    var isDoubleScanFlow: Boolean

    var userName: String?

    var drlStateIT: DrlState

    var drlStateEU: DrlState

    /**
     *
     * This method clears all values from the Shared Preferences file.
     */
    fun clear()

    fun clearDrlPrefs(drlFlowType: DrlFlowType)

    fun deleteScanMode()

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


    override var validationRulesJson by StringPreference(
        preferences,
        PrefKeys.KEY_VALIDATION_RULES,
        ""
    )

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

    override var isDrlSyncActive by BooleanPreference(
        preferences,
        PrefKeys.KEY_IS_DRL_SYNC_ACTIVE,
        true
    )

    override var isDrlSyncActiveEU by BooleanPreference(
        preferences,
        PrefKeys.KEY_IS_DRL_SYNC_ACTIVE_EU,
        true
    )

    override var shouldInitDownload by BooleanPreference(
        preferences,
        PrefKeys.KEY_SHOULD_INIT_DOWNLOAD,
        false
    )

    override var maxRetryNumber by IntPreference(preferences, PrefKeys.KEY_MAX_RETRY_NUM, 1)

    override var scanMode by StringPreference(preferences, PrefKeys.KEY_SCAN_MODE, null)

    override var isDoubleScanFlow by BooleanPreference(
        preferences,
        PrefKeys.KEY_IS_DOUBLE_SCAN_FLOW,
        false
    )
    override var userName by StringPreference(preferences, PrefKeys.KEY_USER_NAME, "")


    override var drlStateIT: DrlState by DrlStatePreference(preferences, PrefKeys.KEY_DRL_STATE_IT, null)

    override var drlStateEU: DrlState by DrlStatePreference(preferences, PrefKeys.KEY_DRL_STATE_EU, null)


    override fun clear() {
        preferences.value.edit { clear() }
    }

    override fun clearDrlPrefs(drlFlowType: DrlFlowType) {
        preferences.value.edit {
            if (drlFlowType == DrlFlowType.IT) remove(PrefKeys.KEY_DRL_STATE_IT) else remove(PrefKeys.KEY_DRL_STATE_EU)
            remove(PrefKeys.AUTH_TO_RESUME)
            remove(PrefKeys.AUTHORIZED_TO_DOWNLOAD)
            remove(PrefKeys.KEY_IS_DRL_SYNC_ACTIVE)
            remove(PrefKeys.KEY_IS_DRL_SYNC_ACTIVE_EU)
            remove(PrefKeys.KEY_SHOULD_INIT_DOWNLOAD)
            remove(PrefKeys.KEY_MAX_RETRY_NUM)
        }
    }

    override fun deleteScanMode() {
        preferences.value.edit {
            remove(PrefKeys.KEY_SCAN_MODE)
        }
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
    private val defaultValue: String?
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

class DrlStatePreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, DrlState> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): DrlState {
        preferences.value.getString(name, defaultValue)?.let {
            return Gson().fromJson(it, DrlState::class.java)
        } ?: return DrlState()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: DrlState) {
        preferences.value.edit { putString(name, Gson().toJson(value)) }
    }
}
