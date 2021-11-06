package it.ministerodellasalute.verificaC19sdk.model

import java.util.Date

/**
 *
 * This data class represents the information contained in the scanned certification in an easier
 * and shorter model than [CertificateModel].
 *
 */
data class CertificateSimple(
    var person: SimplePersonModel? = SimplePersonModel(),
    var dateOfBirth: String? = null,
    var certificateStatus: CertificateStatus? = null,
    var timeStamp: Date? = null
)

data class SimplePersonModel(
    var standardisedFamilyName: String? = null,
    var familyName: String?= null,
    var standardisedGivenName: String?= null,
    var givenName: String?= null
)