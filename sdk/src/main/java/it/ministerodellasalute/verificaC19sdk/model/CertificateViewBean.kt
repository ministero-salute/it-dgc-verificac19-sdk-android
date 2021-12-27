package it.ministerodellasalute.verificaC19sdk.model

import java.util.Date

/**
 *
 * This data class represents the information contained in the scanned certification in an easier
 * and shorter model than [CertificateModel].
 *
 */
data class CertificateViewBean(
    var person: PersonModel? = PersonModel(),
    var dateOfBirth: String? = null,
    var certificateStatus: CertificateStatus? = null,
    var timeStamp: Date? = null
)

