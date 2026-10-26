package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.data.mail.SpkiPinningTrustManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * The pinning trust manager (data/mail/SpkiPinningTrustManager.kt).
 *
 * The delegate is stubbed so the matrix under test is purely the
 * pinning contract: platform validation FIRST (a delegate rejection
 * always wins, pin or no pin), pin presence SECOND (accept requires
 * both). The harness certs and their pins are the same known-answer
 * pair as CertificatePinsTest.
 */
class SpkiPinningTrustManagerTest {

    private class StubDelegate(private val accept: Boolean) : X509TrustManager {
        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            if (!accept) throw CertificateException("stub delegate rejection")
        }

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {}

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }

    @Test
    fun acceptsWhenDelegateAcceptsAndPinIsInChain() {
        val manager = SpkiPinningTrustManager(setOf(PIN_A), StubDelegate(accept = true))
        manager.checkServerTrusted(chain(), "RSA") // must not throw
    }

    @Test
    fun rejectsWhenNoPinMatches() {
        val manager = SpkiPinningTrustManager(setOf(PIN_B), StubDelegate(accept = true))
        try {
            manager.checkServerTrusted(arrayOf(certA()), "RSA")
            fail("Expected a CertificateException for a chain with no pinned anchor")
        } catch (expected: CertificateException) {
            // pinning failure — the whole point of the class
        }
    }

    @Test
    fun delegateRejectionWinsEvenWithAMatchingPin() {
        val manager = SpkiPinningTrustManager(setOf(PIN_A), StubDelegate(accept = false))
        try {
            manager.checkServerTrusted(chain(), "RSA")
            fail("Expected the delegate's rejection to propagate")
        } catch (expected: CertificateException) {
            // platform said no — the pin may never resurrect a bad chain
        }
    }

    @Test
    fun platformDelegateExists() {
        assertNotNull(SpkiPinningTrustManager.platformTrustManager())
    }

    private fun chain(): Array<X509Certificate> = arrayOf(certB(), certA())

    private fun certA(): X509Certificate = parse(CERT_A_PEM)

    private fun certB(): X509Certificate = parse(CERT_B_PEM)

    private fun parse(pem: String): X509Certificate {
        val factory = CertificateFactory.getInstance("X.509")
        return ByteArrayInputStream(pem.toByteArray(Charsets.UTF_8)).use {
            factory.generateCertificate(it) as X509Certificate
        }
    }

    private companion object {
        const val PIN_A = "iAGo8Zl+mu3/QHsZjbZGclQJ2UIOXUlg1BOVH/DnHH8="
        const val PIN_B = "Pd/T8QI/VyN3MAHIZucRvzxQ8tmqLygEphREIMYH9KA="

        // Same throwaway harness certs as CertificatePinsTest
        // (openssl-verified pins above). Duplicated deliberately:
        // each test file stands alone.
        const val CERT_A_PEM = """-----BEGIN CERTIFICATE-----
MIIBRzCB76ADAgECAggH+vW7cUN8wTAKBggqhkjOPQQDAjAZMRcwFQYDVQQDEw5I
YXJuZXNzIENlcnQgQTAeFw0yNjEwMjYwMjA4MjJaFw0zNjEwMjMwMjA4MjJaMBkx
FzAVBgNVBAMTDkhhcm5lc3MgQ2VydCBBMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcD
QgAEb30URi8pAmk4sLZHCqX6p1IBnIJSt0dV9d6/zW5IkAoPPYbHI8Wz5R4a6ReU
Ig7A26q9CLmWGi2q8AsGGybopaMhMB8wHQYDVR0OBBYEFHgO2M52A1Vq2zxIqHFu
R3s0OM3IMAoGCCqGSM49BAMCA0cAMEQCIFPkpJBHggZ9iu3vVC/tWNdPvM2xXBwO
80Df6z8r7UdxAiBT8rlWOI2rRI9Ocu0XRMAEDB9Z1oxYnYizEcmpjgw6xg==
-----END CERTIFICATE-----"""

        const val CERT_B_PEM = """-----BEGIN CERTIFICATE-----
MIIC1jCCAb6gAwIBAgIJAI9s3EVl+M0RMA0GCSqGSIb3DQEBCwUAMBkxFzAVBgNV
BAMTDkhhcm5lc3MgQ2VydCBCMB4XDTI2MTAyNjAyMDgyNFoXDTM2MTAyMzAyMDgy
NFowGTEXMBUGA1UEAxMOSGFybmVzcyBDZXJ0IEIwggEiMA0GCSqGSIb3DQEBAQUA
A4IBDwAwggEKAoIBAQCiOcmS4XkEAdhUGBavRo2Ya+XEoCy0q4wi6DXOxnpaKC9X
NVd+fPPMP5FIcKJMAg6abIin7rlxyjAGxhA0D3N0LpSxPaVMLwNPZ9RM+bPC0maF
QEJjTIXd5ORm9FHGbufSH0A+4exhYeRs1l//eSxq28CZ14wvtjkfQc1tGBJfqHZ2
cAG7pCQBscmYgVEkyzwL20yIz4QW9UJJJnVV+/X7ifUCFCAdZYQHHZRd6aR+u+Q7
gtrUaTbttLrEVzPMAAk2OFnnwMWaCK/jYNMyW4ok1xTa//XWVCLIDrxd2a6idzQr
HQun2B+V6vA7MV6A3s/zFlU6vAGu6UMU0BJ+MF3FAgMBAAGjITAfMB0GA1UdDgQW
BBRrChL/z4Nqw7/PxBXajL+UPditETANBgkqhkiG9w0BAQsFAAOCAQEAFcJOLEpP
jYeE8d3Lv4zjqio3ElrbT4QZ6YQT/w/b1y8tLjVShN1ZH5XsZkFsPcIdQ6K818Rc
LdxaFF3+u1tunDkZ12zNRgZFKwl7w7pTyLRe1lBLjkX5aEADCUxZJRqMiB0YpTQf
YZhRWwYQZUA6NE4uewopCrK1DrEd3CwWfWedlppxXc4j4ffRm2lUoOqseibNaHv7
aFWoqy+BG+PG1eHrPBeGryocy7kmMKVvm0T3RxviY29aoVT3sNhJlw2Oz+j19cHF
tGdooyrdOG4tP7DxdbXhv4+EV5r2YQrdAP4/Hwm57Jy1tJ1gCMgBZ1RAdqeCxbab
FjwRV1mv3M02lw==
-----END CERTIFICATE-----"""
    }
}
