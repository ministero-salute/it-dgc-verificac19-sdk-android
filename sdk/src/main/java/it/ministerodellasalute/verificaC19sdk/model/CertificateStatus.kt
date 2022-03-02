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
 */

package it.ministerodellasalute.verificaC19sdk.model

/**
 *
 * This enum class defines all the possible status of certifications after their verification.
 *
 */
enum class CertificateStatus(val value: String) {
    NOT_VALID("notValid"),
    NOT_VALID_YET("notValidYet"),
    VALID("valid"),
    EXPIRED("expired"),
    REVOKED("revoked"),
    NOT_EU_DCC("notEuDCC"),
    TEST_NEEDED("verificationIsNeeded");
}

fun CertificateStatus.applyFullModel(fullModel: Boolean): CertificateStatus {
    return if (!fullModel && this == CertificateStatus.NOT_VALID_YET) {
        CertificateStatus.NOT_VALID
    } else this
}

fun CertificateStatus.isANonValidCertificate(): Boolean {
    val list = listOf(CertificateStatus.NOT_VALID, CertificateStatus.NOT_VALID_YET, CertificateStatus.EXPIRED, CertificateStatus.REVOKED)

    return list.contains(this)
}