package net.sentinelsafeguard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * LOGO RULE regression pin (owner order 2026-11-08: "You use my
 * logo not your invention"). Before 1.5.3 the launcher foreground
 * was a drawn vector shield-and-checkmark — an invented brand
 * mark. 1.5.3 installs the package built by generate_icon.py
 * from the owner's real logo (your_files/brand/
 * sentinel-safeguard-logo.jpg, the full lockup). These tests pin
 * the resource-level facts so the invented mark can never
 * silently return: the vector file stays absent, the adaptive
 * icon resolves to the raster logo PNGs, and every launcher PNG
 * stays byte-identical to the owner-logo package (a swap must be
 * a deliberate act that edits the assets and this test together).
 */
class LauncherIconTest {

    private fun resDir(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res"),
        )
        return candidates.firstOrNull { File(it, "values/colors.xml").isFile }
            ?: throw AssertionError("res dir not found from ${File(".").absolutePath}")
    }

    private fun sha256(f: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun drawnShieldVectorForegroundStaysRetired() {
        assertFalse(
            "the invented shield-and-checkmark vector must never return",
            File(resDir(), "drawable/ic_launcher_foreground.xml").exists(),
        )
    }

    @Test
    fun adaptiveIconResolvesToRasterLogoForeground() {
        val xml = File(resDir(), "mipmap-anydpi-v26/ic_launcher.xml").readText()
        assertTrue(xml.contains("@mipmap/ic_launcher_foreground"))
        assertFalse(xml.contains("@drawable/ic_launcher_foreground"))
        assertTrue(xml.contains("@color/ic_launcher_background"))
    }

    @Test
    fun launcherPngsMatchOwnerLogoPackageBytes() {
        val expected = mapOf(
            "mipmap-mdpi/ic_launcher.png" to
                "53049c39d64fa496aa63f95be279c91133f3278496b5d20026460ede2cae0745",
            "mipmap-mdpi/ic_launcher_foreground.png" to
                "b9e62593678640e448acb23fae180c4414776de442a5457a7ba6ba12bd7d3460",
            "mipmap-hdpi/ic_launcher.png" to
                "d594d82a1cd56a2a11eeeb5191ff12a27243142ae48debb3d96c4c81718442e2",
            "mipmap-hdpi/ic_launcher_foreground.png" to
                "9c83c6434fb66d5bda3da7613b99f3a42479132ec8384905f9bf8647c3ea685f",
            "mipmap-xhdpi/ic_launcher.png" to
                "54901be614f830301efe95801f8ecb7c5ab9d5c2bcdfc6f8a21eb4116ef98976",
            "mipmap-xhdpi/ic_launcher_foreground.png" to
                "0bc088ee4b25f15c72ced71f911629560806c14435758e822ae39cd89c288e5b",
            "mipmap-xxhdpi/ic_launcher.png" to
                "7d61b21afbc011093eb1d14e38f34760c334d5913758909cd8d70dbc8719fd5b",
            "mipmap-xxhdpi/ic_launcher_foreground.png" to
                "6a636f5b16f1ed3a140da60c33724a9588c330a8a22999328c966609fb3b4790",
            "mipmap-xxxhdpi/ic_launcher.png" to
                "fb46c90f30386859abfbea5aca1677175e2f8f62de7ccfcbd48cab53ec477061",
            "mipmap-xxxhdpi/ic_launcher_foreground.png" to
                "09b5de8adf9be11eb4dff6990b256c07e6c5c406bebe83a552410f0036474093",
        )
        for ((rel, want) in expected) {
            val f = File(resDir(), rel)
            assertTrue("missing launcher asset $rel", f.isFile)
            assertEquals("launcher asset $rel drifted from the owner logo package", want, sha256(f))
        }
    }

    @Test
    fun adaptiveBackgroundIsBrandBlack() {
        val colors = File(resDir(), "values/colors.xml").readText()
        assertTrue(
            colors.contains(
                "<color name=\"ic_launcher_background\">#060806</color>",
            ),
        )
    }

    @Test
    fun notificationIconIsDerivedLogoSilhouetteNotDrawnShield() {
        assertFalse(
            "the drawn shield notification vector must never return",
            File(resDir(), "drawable/ic_stat_sentinel.xml").exists(),
        )
        val png = File(resDir(), "drawable-nodpi/ic_stat_sentinel.png")
        assertTrue("notification silhouette PNG missing", png.isFile)
        assertEquals(
            "notification silhouette drifted from the derived-from-logo asset",
            "09424f66b7e1397a19370fcb40f1a82e0f635830b81c76a05369c6a881ffd1e7",
            sha256(png),
        )
    }
}
