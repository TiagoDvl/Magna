package com.tick.magna.ui.core.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CtaButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    tint: Color = Color.Black,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            painter = icon,
            tint = tint,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun CtaButtonPreview() {
    CtaButton(
        icon = painterResource(Res.drawable.ic_chevron_right),
        tint = Color.Black,
        onClick = {}
    )
}