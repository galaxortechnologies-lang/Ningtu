package com.nigutu.app.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.nigutu.app.domain.model.ThemeMode

// Brand palette — deep black and charcoal surfaces with violet and cyan accents.
val NigutuBlack = Color(0xFF07070A)
val NigutuCharcoal = Color(0xFF121319)
val NigutuElevated = Color(0xFF1A1B23)
val NigutuViolet = Color(0xFF8B5CF6)
val NigutuCyan = Color(0xFF22D3EE)
val NigutuPink = Color(0xFFF472B6)
val NigutuTextPrimary = Color(0xFFF5F5F7)
val NigutuTextSecondary = Color(0xFF9CA0AE)

private val DarkColors = darkColorScheme(
    primary = NigutuViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1E4A),
    onPrimaryContainer = NigutuTextPrimary,
    secondary = NigutuCyan,
    onSecondary = NigutuBlack,
    tertiary = NigutuPink,
    background = NigutuBlack,
    onBackground = NigutuTextPrimary,
    surface = NigutuCharcoal,
    onSurface = NigutuTextPrimary,
    surfaceVariant = NigutuElevated,
    onSurfaceVariant = NigutuTextSecondary,
    outline = Color(0xFF2E2F3A),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D3EF5),
    onPrimary = Color.White,
    secondary = Color(0xFF0E7490),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF101018),
    surface = Color.White,
    onSurface = Color(0xFF101018),
    surfaceVariant = Color(0xFFEDEBF5),
    onSurfaceVariant = Color(0xFF55576A),
    outline = Color(0xFFC9C7D6),
)

private val NigutuTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

/** Applies the Nigutu theme, honouring the user's stored [themeMode]. */
@Composable
fun NigutuTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NigutuTypography,
        content = content,
    )
}
