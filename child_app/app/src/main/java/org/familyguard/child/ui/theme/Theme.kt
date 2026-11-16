package org.familyguard.child.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ChildPrimaryGreen,
    onPrimary = SurfaceWhite,
    secondary = ChildAccentBlue,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    error = DangerRed
)

@Composable
fun FamilyGuardChildTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
