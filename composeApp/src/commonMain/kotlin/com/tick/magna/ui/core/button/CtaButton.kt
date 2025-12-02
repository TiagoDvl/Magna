package com.tick.magna.ui.core.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class CtaButtonSize {
    SMALL, MEDIUM, LARGE;

    fun getButtonSize(): Dp {
        return when (this) {
            SMALL -> 40.dp
            MEDIUM -> 50.dp
            LARGE -> 65.dp
        }
    }

    fun getIconSize(): Dp {
        return when (this) {
            SMALL -> 20.dp
            MEDIUM -> 25.dp
            LARGE -> 30.dp
        }
    }

}


@Composable
fun CtaButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    tint: Color = Color.White,
    size: CtaButtonSize = CtaButtonSize.SMALL,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier.size(size.getButtonSize()),
        colors = IconButtonDefaults.iconButtonColors().copy(
            containerColor = containerColor,
            contentColor = tint
        ),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(size.getIconSize()),
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