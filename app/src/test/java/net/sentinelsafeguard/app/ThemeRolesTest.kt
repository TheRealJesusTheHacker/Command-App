package net.sentinelsafeguard.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AUDIT-171 H-03/M-09: the Material 3 theme must set EVERY color
 * role explicitly. An unset role takes the Material baseline
 * (purple-tinted) value — the exact mechanism that once shipped a
 * purple FAB in a sibling app, and that left this app's selected
 * FilterChips (secondaryContainer), dialogs, drawer sheet and top
 * bars (surfaceContainer family) on baseline colors.
 *
 * AUDIT-171 H-05: the theme's error role is the TEXT-side red and
 * must be the bright, contrast-passing value; the deep red stays
 * for filled surfaces (SentinelDanger / errorContainer).
 */
class ThemeRolesTest {

    private fun themeSource(): String {
        val candidates = listOf(
            File("src/main/java/net/sentinelsafeguard/app/ui/theme/Theme.kt"),
            File("app/src/main/java/net/sentinelsafeguard/app/ui/theme/Theme.kt"),
        )
        val found = candidates.firstOrNull { it.isFile }
            ?: throw AssertionError("Theme.kt not found from ${File(".").absolutePath}")
        return found.readText()
    }

    @Test
    fun everyColorSchemeRoleIsSetExplicitly() {
        val src = themeSource()
        val scheme = src.substringAfter("darkColorScheme(").substringBefore("\n)")
        val roles = listOf(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground", "surface", "onSurface",
            "surfaceVariant", "onSurfaceVariant", "surfaceTint",
            "surfaceDim", "surfaceBright",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
            "surfaceContainerHigh", "surfaceContainerHighest",
            "inverseSurface", "inverseOnSurface", "inversePrimary",
            "outline", "outlineVariant", "scrim",
        )
        val missing = roles.filter { role ->
            !Regex("""\b$role\s*=""").containsMatchIn(scheme)
        }
        assertTrue("Unset Material roles (baseline purple risk): $missing", missing.isEmpty())
    }

    @Test
    fun errorRoleIsTheBrightTextValue() {
        val src = themeSource()
        assertTrue(
            "theme error role must be SentinelDangerBright",
            Regex("""\berror\s*=\s*SentinelDangerBright""").containsMatchIn(src),
        )
        assertTrue(
            "errorContainer keeps the deep filled red",
            Regex("""\berrorContainer\s*=\s*SentinelDanger\b""").containsMatchIn(src),
        )
    }

    @Test
    fun noPurpleFamilyHexAnywhereInTheme() {
        val src = themeSource()
        // The estate's forbidden-baseline guard: no Material purple
        // family values may appear in the theme file at all.
        val purples = listOf("6750A4", "D0BCFF", "7D5260", "EFB8C8", "4A4458", "49454F")
        val found = purples.filter { src.contains(it, ignoreCase = true) }
        assertTrue("Purple baseline values present: $found", found.isEmpty())
    }
}
