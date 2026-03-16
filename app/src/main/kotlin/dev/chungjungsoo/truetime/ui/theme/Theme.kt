package dev.chungjungsoo.truetime.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF245FA7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD8E3FF),
        onPrimaryContainer = Color(0xFF001B3E),
        secondary = Color(0xFF52606F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD6E4F6),
        onSecondaryContainer = Color(0xFF0E1D2A),
        tertiary = Color(0xFF006A62),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF76F8EB),
        onTertiaryContainer = Color(0xFF00201C),
        background = Color(0xFFF7F9FD),
        onBackground = Color(0xFF171C21),
        surface = Color(0xFFF7F9FD),
        onSurface = Color(0xFF171C21),
        surfaceVariant = Color(0xFFDDE3EB),
        onSurfaceVariant = Color(0xFF41484D),
        outline = Color(0xFF71787E)
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFA9C7FF),
        onPrimary = Color(0xFF003061),
        primaryContainer = Color(0xFF004789),
        onPrimaryContainer = Color(0xFFD8E3FF),
        secondary = Color(0xFFBAC8DA),
        onSecondary = Color(0xFF243240),
        secondaryContainer = Color(0xFF3A4857),
        onSecondaryContainer = Color(0xFFD6E4F6),
        tertiary = Color(0xFF56DBCF),
        onTertiary = Color(0xFF003731),
        tertiaryContainer = Color(0xFF005048),
        onTertiaryContainer = Color(0xFF76F8EB),
        background = Color(0xFF0F1419),
        onBackground = Color(0xFFE0E2E7),
        surface = Color(0xFF0F1419),
        onSurface = Color(0xFFE0E2E7),
        surfaceVariant = Color(0xFF41484D),
        onSurfaceVariant = Color(0xFFC1C7CF),
        outline = Color(0xFF8B9298)
    )

private val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(12),
        small = RoundedCornerShape(16),
        medium = RoundedCornerShape(20),
        large = RoundedCornerShape(28),
        extraLarge = RoundedCornerShape(32)
    )

private fun withFontPadding(style: TextStyle): TextStyle = style.copy(
    platformStyle = PlatformTextStyle(includeFontPadding = true)
)

private val AppTypography =
    Typography().let { base ->
        Typography(
            displayLarge = withFontPadding(base.displayLarge),
            displayMedium = withFontPadding(base.displayMedium),
            displaySmall = withFontPadding(base.displaySmall),
            headlineLarge = withFontPadding(base.headlineLarge),
            headlineMedium = withFontPadding(base.headlineMedium),
            headlineSmall = withFontPadding(base.headlineSmall),
            titleLarge = withFontPadding(base.titleLarge),
            titleMedium = withFontPadding(base.titleMedium),
            titleSmall = withFontPadding(base.titleSmall),
            bodyLarge = withFontPadding(base.bodyLarge),
            bodyMedium = withFontPadding(base.bodyMedium),
            bodySmall = withFontPadding(base.bodySmall),
            labelLarge = withFontPadding(base.labelLarge),
            labelMedium = withFontPadding(base.labelMedium),
            labelSmall = withFontPadding(base.labelSmall)
        )
    }

@Composable
fun TrueTimeTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
                dynamicDarkColorScheme(context)
            }

            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColorScheme
            }

            else -> {
                LightColorScheme
            }
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
