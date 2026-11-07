package net.sentinelsafeguard.app

import net.sentinelsafeguard.app.ui.Phase5Logic
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetResolveLogicTest {
    @Test fun bareCodePassesThrough() {
        assertEquals("SCA-abc123", Phase5Logic.assetCodeFromInput("  SCA-abc123 "))
    }
    @Test fun labelUrlExtractsCode() {
        assertEquals("SCA-xyz789", Phase5Logic.assetCodeFromInput("https://sentinelsafeguard.net/a/SCA-xyz789"))
        assertEquals("SCA-xyz789", Phase5Logic.assetCodeFromInput("https://sentinelsafeguard.net/a/SCA-xyz789/"))
    }
    @Test fun blankStaysBlank() {
        assertEquals("", Phase5Logic.assetCodeFromInput("   "))
    }
}
