package com.tick.magna.ui.core.button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.app_name
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class MagnaButtonSize {
    SMALL, MEDIUM, LARGE;

    fun getWidth(): Dp {
        return when (this) {
            SMALL -> 80.dp
            MEDIUM -> 100.dp
            LARGE -> 160.dp
        }
    }

    fun getIconSize(): Dp {
        return when (this) {
            SMALL -> 16.dp
            MEDIUM -> 20.dp
            LARGE -> 32.dp
        }
    }

    @Composable
    fun getFont(): TextStyle {
        return when (this) {
            SMALL -> MaterialTheme.typography.labelLarge
            MEDIUM -> MaterialTheme.typography.bodyLarge
            LARGE -> MaterialTheme.typography.titleLarge
        }
    }
}
@Composable
fun MagnaButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: MagnaButtonSize = MagnaButtonSize.MEDIUM,
    icon: Painter? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit
) {
    val dimensions = LocalDimensions.current

    Button(
        onClick = onClick,
        modifier = modifier.width(size.getWidth()),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = dimensions.grid8, vertical = dimensions.grid4)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid2)
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.size(size.getIconSize()),
                    painter = icon,
                    contentDescription = null
                )
            }

            BaseText(
                modifier = Modifier.weight(1F),
                text = text,
                style = size.getFont(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun MagnaButtonPreview() {
    Column {
        MagnaButtonSize.entries.forEach { size ->
            MagnaButton(
                icon = painterResource(Res.drawable.ic_chevron_right),
                size = size,
                text = stringResource(Res.string.app_name),
                onClick = {}
            )
        }
    }
}