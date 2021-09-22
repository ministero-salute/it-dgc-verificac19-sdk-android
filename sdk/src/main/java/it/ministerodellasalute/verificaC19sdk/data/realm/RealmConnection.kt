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
 *  Created by nicolamcornelio on 22/09/2021, 14:49
 */

package it.ministerodellasalute.verificaC19sdk.data.realm

import android.content.Context
import android.util.Log
import io.realm.Realm
import io.realm.RealmConfiguration

class RealmConnection {

    companion object {
        private const val realmName: String = "RevokedPasses"
        private val config = RealmConfiguration.Builder().name(realmName).build()

        fun initializeRealmLibrary(context: Context) {
            Realm.init(context)
            Log.i("Realm", "DB initialized.")
        }

        fun openRealm(): Realm {
            //val realmName: String = "RevokedPasses"
            //val config = RealmConfiguration.Builder().name(realmName).build()

            return Realm.getInstance(config)
        }

        fun dropRealm() {
            Realm.deleteRealm(config)
        }
    }
}