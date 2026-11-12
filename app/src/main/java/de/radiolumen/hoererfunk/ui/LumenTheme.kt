package de.radiolumen.hoererfunk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF1C1915)
val Paper = Color(0xFFF6F1E7)
val Card = Color(0xFFFFFCF7)
val Line = Color(0xFFE4D8C4)
val Teal = Color(0xFF1F6F5B)
val TealDark = Color(0xFF144C3E)
val Urgent = Color(0xFF9E2B2B)
val Muted = Color(0xFF6E6458)
val Live = Color(0xFFB4332A)

private val Sans = FontFamily.SansSerif

private val Colors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Teal,
    onPrimaryContainer = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Teal,
    onSecondaryContainer = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    error = Urgent,
)

private fun TextStyle.sans() = copy(fontFamily = Sans)

private val Type = Typography().run {
    copy(
        displayLarge = displayLarge.sans(),
        displayMedium = displayMedium.sans(),
        displaySmall = displaySmall.sans(),
        headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 34.sp),
        headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    )
}

@Composable
fun LumenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type) {
        Box(Modifier.fillMaxSize().background(Paper)) {
            content()
        }
    }
}
