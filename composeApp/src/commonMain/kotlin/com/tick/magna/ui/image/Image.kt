package com.tick.magna.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
fun Image(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageUrl: String?,
    placeholder: Painter? = null,
    contentDescription: String? = null,
) {
    AsyncImage(
        modifier = modifier,
        contentScale = contentScale,
        model = imageUrl,
        contentDescription = contentDescription,
        placeholder = placeholder
    )
}