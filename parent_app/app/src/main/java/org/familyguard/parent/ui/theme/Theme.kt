package org.familyguard.parent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = PrimaryBlueVariant,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    error = DangerRed
)

@Composable
fun FamilyGuardParentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
