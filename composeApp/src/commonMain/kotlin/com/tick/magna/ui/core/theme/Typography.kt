package com.tick.magna.ui.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.magna_sans_display_bold
import magna.composeapp.generated.resources.magna_sans_display_extra_light
import magna.composeapp.generated.resources.magna_sans_display_light
import magna.composeapp.generated.resources.magna_sans_display_medium
import magna.composeapp.generated.resources.magna_sans_display_regular
import magna.composeapp.generated.resources.magna_sans_display_semi_bold
import org.jetbrains.compose.resources.Font

@Composable
fun magnaTypography(): Typography {
    val magnaFontFamily = FontFamily(
        Font(Res.font.magna_sans_display_extra_light, FontWeight.ExtraLight, FontStyle.Normal),
        Font(Res.font.magna_sans_display_light, FontWeight.Light, FontStyle.Normal),
        Font(Res.font.magna_sans_display_medium, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.magna_sans_display_regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.magna_sans_display_semi_bold, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.magna_sans_display_bold, FontWeight.Bold, FontStyle.Normal),
    )

    return Typography(
        displayLarge = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),

        // Headlines
        headlineLarge = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),

        // Titles
        titleLarge = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),

        bodyLarge = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),

        // Labels
        labelLarge = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = magnaFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}
