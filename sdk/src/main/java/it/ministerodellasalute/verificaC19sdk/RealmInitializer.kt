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
 *  Created by kaizen-7 on 29/12/21, 19:19
 */

package it.ministerodellasalute.verificaC19sdk

import android.content.Context
import androidx.startup.Initializer
import io.realm.Realm
import io.realm.RealmConfiguration
import it.ministerodellasalute.verificaC19sdk.data.VerifierRepositoryImpl
import it.ministerodellasalute.verificaC19sdk.data.local.VerificaC19sdkRealmModule

class RealmInitializer : Initializer<Realm> {

    override fun create(context: Context): Realm {
        Realm.init(context)
        val config = RealmConfiguration.Builder()
            .name(VerifierRepositoryImpl.REALM_NAME)
            .modules(VerificaC19sdkRealmModule())
            .allowQueriesOnUiThread(true)
            .build()
        return Realm.getInstance(config)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

}