package ir.mehdisekoba.sample.ui.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class Palette(
    val background: Color,
    val bone: Color,
    val boneSoft: Color,
    val placeholder: Color,
    val surface: Color,
)

private val PaletteLight = Palette(
    background = Color(0xFFFFFFFF),
    bone = Color(0xFFE6E6EB),
    boneSoft = Color(0xFFEFEFF3),
    placeholder = Color(0xFFC7C7CC),
    surface = Color(0xFFF4F4F6),
)

private val PaletteDark = Palette(
    background = Color(0xFF000000),
    bone = Color(0xFF2C2C2E),
    boneSoft = Color(0xFF1F1F22),
    placeholder = Color(0xFF3A3A3C),
    surface = Color(0xFF151517),
)

@Composable
fun rememberPalette(): Palette = PaletteLight
