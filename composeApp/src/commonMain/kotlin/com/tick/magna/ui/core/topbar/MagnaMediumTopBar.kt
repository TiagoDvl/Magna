package com.tick.magna.ui.core.topbar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.tick.magna.ui.core.theme.MagnaTheme
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.action_back
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaMediumTopBar(
    titleText: String,
    leftIcon: Painter? = null,
    leftIconClick: () -> Unit = {},
    /** Every screen uses this icon to go back; override it if one ever does not. */
    leftIconContentDescription: String = stringResource(Res.string.action_back),
    /** Trailing controls that belong to the screen, such as a favourite toggle. */
    actions: @Composable RowScope.() -> Unit = {},
    /**
     * Makes the bar collapse as the content scrolls under it.
     *
     * Null keeps it static, which is what every screen in this app did until now: there was
     * not one `scrollBehavior` or `nestedScroll` in the whole of commonMain. Handing it in
     * rather than creating it here is deliberate — the same object has to reach the Scaffold's
     * `nestedScroll` modifier, and a bar that owns its own state silently does nothing.
     */
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        actions = actions,
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
                        contentDescription = leftIconContentDescription
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