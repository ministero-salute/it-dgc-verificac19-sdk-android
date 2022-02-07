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
 *  Created by Mykhailo Nester on 4/23/21 9:49 AM
 */

object Versions {



    // Base
    const val gradle = "4.1.3"
    const val kotlin = "1.4.32"
    const val desugar_jdk_libs = "1.1.5"
    const val mockk = "1.11.0"

    const val androidx_core = "1.7.0"
    const val androidx_startup = "1.1.0"
    const val androidx_appcompat = "1.4.0"
    const val androidx_navigation = "2.3.5"
    const val androidx_material = "1.3.0"
    const val androidx_constraint = "2.1.2"
    const val logging_interceptor = "4.9.3"
    const val okhttp = "4.9.3"
    const val kotlinx_coroutines = "1.3.9"
    const val retrofit = "2.9.0"
    const val androidx_room = "2.4.0"
    const val androidx_core_testing = "2.1.0"
    const val test_coroutines = "1.5.0"
    const val gson_converter = "2.9.0"
    const val gson = "2.8.9"

    const val dokka = "1.5.0"

    private const val work_hilt = "1.0.0-beta01"
    const val androidx_hilt_viewmodel = "1.0.0-alpha02"
    const val androidx_hilt_work = work_hilt
    const val androidx_hilt_compiler = work_hilt
    const val androidx_worker_ktx = "2.7.1"
    const val hilt_version = "2.33-beta"

    // QR
    const val zxing = "4.2.0"
    const val guave_conflict_resolver_version = "9999.0-empty-to-avoid-conflict-with-guava"

    // Decoder
    const val kotlin_reflect = "1.4.32"
    const val jackson_cbor = "2.12.3"
    const val java_cose = "1.1.0"
    const val bouncy_castle = "1.68"
    const val json_validation = "2.2.14"
    const val json_validation_rhino = "1.0"
    const val junit = "4.13.1"
    const val junit_jupiter = "5.7.1"
    const val hamcrest = "2.2"
}