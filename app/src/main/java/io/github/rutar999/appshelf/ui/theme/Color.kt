package io.github.rutar999.appshelf.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Dynamic Color（Android 12 以降で壁紙から配色を作る仕組み）が使えないとき、
 * および設定で OFF にしたときに使うブランド配色。
 */

private val Blue40 = Color(0xFF3A5A8C)
private val Blue80 = Color(0xFFACC7FF)

val LightColors = lightColorScheme(
    primary = Blue40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF565E71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF715573),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFCD7FB),
    onTertiaryContainer = Color(0xFF29132D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0)
)

val DarkColors = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF1D4489),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDEBCDE),
    onTertiary = Color(0xFF402843),
    tertiaryContainer = Color(0xFF583E5B),
    onTertiaryContainer = Color(0xFFFCD7FB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F)
)

/**
 * タグに選べる色。
 * ライト／ダークどちらの背景でも文字が読めるよう、中間の明度でそろえている。
 */
val TagColors: List<Int> = listOf(
    0xFF4E79A7.toInt(), // 青
    0xFF59A14F.toInt(), // 緑
    0xFFE15759.toInt(), // 赤
    0xFFF28E2B.toInt(), // 橙
    0xFFB07AA1.toInt(), // 紫
    0xFF76B7B2.toInt(), // 青緑
    0xFFEDC948.toInt(), // 黄
    0xFFFF9DA7.toInt(), // 桃
    0xFF9C755F.toInt(), // 茶
    0xFF7F7F7F.toInt()  // 灰
)
