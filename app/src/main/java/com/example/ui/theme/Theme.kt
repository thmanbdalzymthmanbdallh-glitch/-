package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = WarmSlateSecondary,
    onSecondary = OnWarmSlateSecondary,
    secondaryContainer = WarmSlateContainer,
    onSecondaryContainer = OnWarmSlateContainer,
    tertiary = SaffronGold,
    onTertiary = OnSaffronGold,
    tertiaryContainer = SaffronContainer,
    onTertiaryContainer = OnSaffronContainer,
    background = AlabasterBackground,
    onBackground = WarmCharcoal,
    surface = PureSurface,
    onSurface = WarmCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedStone,
    outline = SoftBorder,
    error = ErrorCrimson,
    errorContainer = ErrorSoftContainer,
    onErrorContainer = OnErrorSoftContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmeraldPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkSaffron,
    onTertiary = DarkOnSaffron,
    tertiaryContainer = DarkSaffronContainer,
    onTertiaryContainer = DarkOnSaffronContainer,
    background = DarkObsidianBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder
)

val MatjariShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MatjariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MatjariTypography,
        shapes = MatjariShapes,
        content = content
    )
}
