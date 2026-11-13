package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.config.AppConfig
import net.sentinelsafeguard.app.config.GateConfig
import net.sentinelsafeguard.app.ui.IntelMapLogic
import net.sentinelsafeguard.app.ui.Wave161Logic
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Desktop-side proof that the SHARED phone layer compiled into
 *  this build behaves: the production host is the phone's, the
 *  gate degrades to "not configured" (no fabricated Access key),
 *  and the shared parsers grade honestly. */
class DesktopSharedLayerTest {

    @Test
    fun productionHostIsThePhones() {
        assertEquals("https://command.sentinelsafeguard.net", AppConfig.api.baseUrl)
    }

    @Test
    fun gateConfigDegradesToNotConfigured() {
        assertEquals("", GateConfig.cfAccessClientId)
        assertEquals("", GateConfig.cfAccessClientSecret)
    }

    @Test
    fun wysAudioRefusalIsDeniedNotFaked() {
        val body = buildJsonObject { put("authorized", false); put("available", false) }
        assertTrue(Wave161Logic.parseWysAudio(body) is Wave161Logic.WysAudioResult.Denied)
    }

    @Test
    fun threatMarkersComeFromThePayloadOnly() {
        val empty = buildJsonObject { put("connected", false) }
        val model = IntelMapLogic.parseThreat(empty)
        assertTrue(model.markers.isEmpty())
        assertTrue(!model.hasContent)
    }
}
