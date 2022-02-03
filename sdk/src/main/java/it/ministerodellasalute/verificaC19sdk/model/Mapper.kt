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
 *  Created by mykhailo.nester on 5/5/21 11:17 PM
 */

package it.ministerodellasalute.verificaC19sdk.model

import dgca.verifier.app.decoder.model.*
import it.ministerodellasalute.verificaC19sdk.util.TimeUtility.toLocalDate
import java.util.*

/**
 *
 * This file is used to map an object type to another one, as defined by the methods it contains.
 *
 */

/**
 *
 * This methods maps a [GreenCertificate] object to a [CertificateModel] instance.
 *
 */
fun GreenCertificate?.toCertificateModel(verificationResult: VerificationResult): CertificateModel {
    return CertificateModel(
        this?.person?.toPersonModel(),
        this?.dateOfBirth,
        this?.vaccinations?.map { it.toVaccinationModel() },
        this?.tests?.map { it.toTestModel() },
        this?.recoveryStatements?.map { it.toRecoveryModel() },
        verificationResult.isValid(),
        verificationResult.cborDecoded
    )
}

/**
 *
 * This methods maps a [RecoveryStatement] object to a [RecoveryModel] instance.
 *
 */
fun RecoveryStatement.toRecoveryModel(): RecoveryModel {
    return RecoveryModel(
        disease,
        dateOfFirstPositiveTest,
        countryOfVaccination,
        certificateIssuer,
        certificateValidFrom,
        certificateValidUntil,
        certificateIdentifier
    )
}

/**
 *
 * This methods maps a [Test] object to a [TestModel] instance.
 *
 */
fun Test.toTestModel(): TestModel {
    return TestModel(
        disease,
        typeOfTest,
        testName,
        testNameAndManufacturer,
        dateTimeOfCollection,
        dateTimeOfTestResult,
        testResult,
        testingCentre,
        countryOfVaccination,
        certificateIssuer,
        certificateIdentifier,
        getTestResultType().toTestResult()
    )
}

/**
 *
 * This methods maps a [Test] object to a [TestResult] instance.
 *
 */
fun Test.TestResult.toTestResult(): TestResult {
    return when (this) {
        Test.TestResult.DETECTED -> TestResult.DETECTED
        Test.TestResult.NOT_DETECTED -> TestResult.NOT_DETECTED
    }
}

/**
 *
 * This methods maps a [Vaccination] object to a [VaccinationModel] instance.
 *
 */
fun Vaccination.toVaccinationModel(): VaccinationModel {
    return VaccinationModel(
        disease,
        vaccine,
        medicinalProduct,
        manufacturer,
        doseNumber,
        totalSeriesOfDoses,
        dateOfVaccination,
        countryOfVaccination,
        certificateIssuer,
        certificateIdentifier
    )
}

/**
 *
 * This methods maps a [Person] object to a [PersonModel] instance.
 *
 */
fun Person.toPersonModel(): PersonModel {
    return PersonModel(
        standardisedFamilyName,
        familyName.orEmpty(),
        standardisedGivenName.orEmpty(),
        givenName.orEmpty()
    )
}

fun CertificateModel.toCertificateViewBean(
    status: CertificateStatus
): CertificateViewBean {
    return CertificateViewBean(
        person,
        dateOfBirth,
        status,
        Date(System.currentTimeMillis())
    )
}