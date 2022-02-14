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
 *  Created by nicolamcornelio on 1/19/22, 10:01 AM
 */

package it.ministerodellasalute.verificaC19sdk.model.validation

import it.ministerodellasalute.verificaC19sdk.model.CertificateModel
import it.ministerodellasalute.verificaC19sdk.model.CertificateStatus
import it.ministerodellasalute.verificaC19sdk.model.validation.RuleSet

interface ValidationStrategy {

    fun checkCertificate(certificateModel: CertificateModel, ruleSet: RuleSet): CertificateStatus
}