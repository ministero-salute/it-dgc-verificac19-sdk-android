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
 *  Created by nicolamcornelio on 1/10/22 5:12 PM
 */

package it.ministerodellasalute.verificaC19sdk.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Exemption(

    @SerializedName("tg")
    val disease: String,

    @SerializedName("co")
    val countryOfVaccination: String,

    @SerializedName("is")
    val certificateIssuer: String,

    @SerializedName("ci")
    val certificateIdentifier: String,

    @SerializedName("df")
    val certificateValidFrom: String,

    @SerializedName("du")
    val certificateValidUntil: String?

) : Serializable