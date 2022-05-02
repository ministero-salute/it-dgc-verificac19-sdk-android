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
 *  Created by kaizen-7 on 02/05/22, 12:58
 */

package it.ministerodellasalute.verificaC19sdk.model.drl
sealed class DownloadState {
    data class RequiresConfirm(val totalSize: Float) : DownloadState()
    object ResumeAvailable : DownloadState()
    object Complete : DownloadState()
    object Downloading : DownloadState()
    object DownloadAvailable : DownloadState()
}
