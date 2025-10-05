package com.tick.magna.ui.avatar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.tick.magna.ui.image.Image
import com.tick.magna.ui.theme.LocalDimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    photoUrl: String? = null,
    placeholder: Painter? = null
) {
    Card(
        modifier = modifier.size(LocalDimensions.current.grid40),
        shape = CircleShape
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            imageUrl = photoUrl,
            placeholder = placeholder,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun AvatarPreview() {
    Avatar(photoUrl = "")
}