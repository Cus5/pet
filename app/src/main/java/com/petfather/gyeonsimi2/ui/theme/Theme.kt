package com.petfather.gyeonsimi2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF2F5D8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E3FF),
    onPrimaryContainer = Color(0xFF0E2F52),
    secondary = Color(0xFF545F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE3F6),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF8A4A79),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F1),
    onTertiaryContainer = Color(0xFF3A0732),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF7F9FD),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF7F9FD),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F3FA),
    surfaceContainer = Color(0xFFEAEEF7),
    surfaceContainerHigh = Color(0xFFE4E8F1),
    surfaceContainerHighest = Color(0xFFDEE2EB),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA2C9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF134574),
    onPrimaryContainer = Color(0xFFD4E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFDAE3F6),
    tertiary = Color(0xFFFFADE3),
    onTertiary = Color(0xFF541C48),
    tertiaryContainer = Color(0xFF6F3360),
    onTertiaryContainer = Color(0xFFFFD7F1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111418),
    onBackground = Color(0xFFE1E2E8),
    surface = Color(0xFF111418),
    onSurface = Color(0xFFE1E2E8),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF272A2F),
    surfaceContainerHighest = Color(0xFF32353A),
)

/** 상태(정상/주의/경고)를 나타내는 보조 색 */
@Immutable
data class StatusColors(
    val good: Color,
    val goodContainer: Color,
    val caution: Color,
    val cautionContainer: Color,
)

private val LightStatus = StatusColors(
    good = Color(0xFF1E6B3A),
    goodContainer = Color(0xFFD5F0DC),
    caution = Color(0xFF8A5100),
    cautionContainer = Color(0xFFFFE3C2),
)
private val DarkStatus = StatusColors(
    good = Color(0xFF8BD8A2),
    goodContainer = Color(0xFF14512A),
    caution = Color(0xFFFFB866),
    cautionContainer = Color(0xFF633B00),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatus }

private val base = Typography()
private val AppTypography = Typography(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    labelSmall = base.labelSmall.copy(fontSize = 11.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun GyeonsimiTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) Dark else Light,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

val NumberStyle: TextStyle
    @Composable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
