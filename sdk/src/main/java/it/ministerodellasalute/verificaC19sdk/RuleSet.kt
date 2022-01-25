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

package it.ministerodellasalute.verificaC19sdk

import com.google.gson.Gson
import it.ministerodellasalute.verificaC19sdk.data.remote.model.Rule
import it.ministerodellasalute.verificaC19sdk.model.ValidationRulesEnum

class RuleSet(rulesJson: String?) {
    private val rules: Array<Rule> = Gson().fromJson(rulesJson, Array<Rule>::class.java)


    fun getVaccineStartDayNotComplete(vaccineType: String): String {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineEndDayNotComplete(vaccineType: String): String {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_NOT_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineStartDayComplete(vaccineType: String): String {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_START_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getVaccineEndDayComplete(vaccineType: String): String {
        return rules.find { it.name == ValidationRulesEnum.VACCINE_END_DAY_COMPLETE.value && it.type == vaccineType }?.value
            ?: run {
                ""
            }
    }

    fun getMolecularTestStartHour(): String {
        return rules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_START_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getMolecularTestEndHour(): String {
        return rules.find { it.name == ValidationRulesEnum.MOLECULAR_TEST_END_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getRapidTestStartHour(): String {
        return rules.find { it.name == ValidationRulesEnum.RAPID_TEST_START_HOUR.value }?.value
            ?: run {
                ""
            }
    }

    fun getRapidTestEndHour(): String {
        return rules.find { it.name == ValidationRulesEnum.RAPID_TEST_END_HOUR.value }?.value
            ?: run {
                ""
            }
    }


    fun getRecoveryCertStartDay(): String {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    fun getRecoveryCertPVStartDay(): String {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_START_DAY.value }?.value
            ?: run {
                ""
            }
    }

    fun getRecoveryCertEndDay(): String {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_END_DAY.value }?.value
            ?: run {
                ""
            }
    }

    fun getRecoveryCertPvEndDay(): String {
        return rules.find { it.name == ValidationRulesEnum.RECOVERY_CERT_PV_END_DAY.value }?.value
            ?: run {
                ""
            }
    }

    fun getAppMinVersion(): String {
        return rules.find { it.name == ValidationRulesEnum.APP_MIN_VERSION.value }?.value ?: run {
            ""
        }
    }

    fun getSDKMinVersion(): String {
        return rules.find { it.name == ValidationRulesEnum.SDK_MIN_VERSION.value }?.value ?: run {
            ""
        }
    }

}