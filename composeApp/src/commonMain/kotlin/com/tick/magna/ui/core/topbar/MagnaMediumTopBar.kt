package com.tick.magna.ui.core.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaMediumTopBar(
    titleText: String,
    leftIcon: Painter? = null,
    leftIconClick: () -> Unit = {},
) {
    MediumTopAppBar(
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        navigationIcon = {
            leftIcon?.let {
                IconButton(
                    onClick = leftIconClick
                ) {
                    Icon(
                        painter = it,
                        contentDescription = null
                    )
                }
            }
        },
        title = {
            val style = if (leftIcon != null)  MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge
            Text(
                text = titleText,
                style = style
            )
        }
    )
}

@Preview
@Composable
private fun PreviewMagnaMediumTopBar() {
    MagnaTheme {
        MagnaMediumTopBar(
            titleText = "Top Bar title",
            leftIcon = painterResource(Res.drawable.ic_arrow_back),
        )
    }
}

@Preview
@Composable
private fun PreviewMagnaMediumTopBarNoIcon() {
    MagnaTheme {
        MagnaMediumTopBar(
            titleText = "Top Bar title",
        )
    }
}