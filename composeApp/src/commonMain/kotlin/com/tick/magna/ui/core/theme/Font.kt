package com.tick.magna.ui.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.magna_sans_display_bold
import magna.composeapp.generated.resources.magna_sans_display_extra_light
import magna.composeapp.generated.resources.magna_sans_display_light
import magna.composeapp.generated.resources.magna_sans_display_medium
import magna.composeapp.generated.resources.magna_sans_display_regular
import magna.composeapp.generated.resources.magna_sans_display_semi_bold
import org.jetbrains.compose.resources.Font

@Composable
fun magnaFontFamily() = FontFamily(
    Font(Res.font.magna_sans_display_extra_light, FontWeight.ExtraLight, FontStyle.Normal),
    Font(Res.font.magna_sans_display_light, FontWeight.Light, FontStyle.Normal),
    Font(Res.font.magna_sans_display_medium, FontWeight.Medium, FontStyle.Normal),
    Font(Res.font.magna_sans_display_regular, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.magna_sans_display_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.magna_sans_display_bold, FontWeight.Bold, FontStyle.Normal),
)