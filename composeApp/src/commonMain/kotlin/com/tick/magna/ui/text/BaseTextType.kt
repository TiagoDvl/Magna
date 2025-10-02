package com.tick.magna.ui.text

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class BaseTextType {
    H1,
    H2,
    H3,
    H4,

    TITLE,
    SUBTITLE,

    BODY,
    BODY2,

    BUTTON,

    CAPTION;

    fun getBaseTextTypeSize(): TextUnit {
        return when (this) {
            H1 -> 32.sp
            H2 -> 28.sp
            H3 -> 24.sp
            H4 -> 20.sp

            TITLE -> 18.sp
            SUBTITLE -> 16.sp

            BODY -> 14.sp
            BODY2 -> 12.sp

            BUTTON -> 14.sp

            CAPTION -> 10.sp
        }
    }
}