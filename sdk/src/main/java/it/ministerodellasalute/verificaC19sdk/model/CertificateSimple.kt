package it.ministerodellasalute.verificaC19sdk.model

data class CertificateSimple(
    var person: SimplePersonModel = SimplePersonModel(),
    var dateOfBirth: String? = null,
    var certificateStatus: CertificateStatus? = null
)

data class SimplePersonModel(
    var standardisedFamilyName: String? = null,
    var familyName: String?= null,
    var standardisedGivenName: String?= null,
    var givenName: String?= null
)