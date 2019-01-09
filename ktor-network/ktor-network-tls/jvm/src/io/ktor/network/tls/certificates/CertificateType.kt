package io.ktor.network.tls.certificates

import io.ktor.network.tls.*

/**
 * Type of client certificate.
 * see also https://tools.ietf.org/html/rfc5246#section-7.4.4
 *
 * @property code numeric algorithm codes
 */
@Suppress("KDocMissingDocumentation")
enum class ClientCertificateType(val code: Byte) {
    RSA_SIGN(1),
    DSS_SIGN(2),
    RSA_FIXED_DH(3),
    DSS_FIXED_DH(4),
    RSA_EPHEMERAL_DH_RESERVED(5),
    DSS_EPHEMERAL_DH_RESERVED(6),
    FORTEZZA_DMS_RESERVED(20);

    companion object {
        /**
         * Find client certificate type by it's numeric [code].
         * @throws TLSExtension if certificate type found.
         */
        fun byCode(code: Byte): ClientCertificateType = values().find { it.code == code }
            ?: throw TLSException("Unknown signature algorithm: $code")
    }
}
