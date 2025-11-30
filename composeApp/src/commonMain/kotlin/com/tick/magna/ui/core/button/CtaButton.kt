package com.tick.magna.ui.core.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
    containerColor: Color = MaterialTheme.colorScheme.primary,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors().copy(
            containerColor = containerColor,
            contentColor = tint
        ),
        onClick = onClick,
    ) {
        Icon(
            painter = icon,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun CtaButtonPreview() {
    CtaButton(
        icon = painterResource(Res.drawable.ic_chevron_right),
        onClick = {}
    )
}