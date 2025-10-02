package com.tick.magna.ui.text

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BaseText(
    modifier: Modifier = Modifier,
    text: String,
    type: BaseTextType,
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = type.getBaseTextTypeSize()
    )
}

@Preview
@Composable
fun BaseTextHeadersPreview() {
    MaterialTheme {
        Column {
            BaseText(
                text = "Base Text - H1",
                type = BaseTextType.H1
            )
            BaseText(
                text = "Base Text - H2",
                type = BaseTextType.H2
            )

            BaseText(
                text = "Base Text - H3",
                type = BaseTextType.H3
            )
            BaseText(
                text = "Base Text - H4",
                type = BaseTextType.H4
            )
        }
    }
}

@Preview
@Composable
fun BaseTextTitleSubtitlePreview() {
    MaterialTheme {
        Column {
            BaseText(
                text = "Base Text - Title",
                type = BaseTextType.TITLE
            )
            BaseText(
                text = "Base Text - Subtitle",
                type = BaseTextType.SUBTITLE
            )

        }
    }
}