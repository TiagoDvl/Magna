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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.onContainer
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
    /**
     * A wordmark for the start of the navigation row, for a screen with no back arrow.
     *
     * MediumTopAppBar always reserves that row — 64dp of the bar's 112 — for navigation and
     * actions. On a pushed screen the arrow fills it; on the Home there is no arrow, so a
     * single trailing icon floated in an otherwise empty band and pushed the title down past
     * it. This puts the app's name where the arrow would be instead of removing the row.
     */
    navigationLabel: String? = null,
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
    /**
     * Paints the bar in the area's own container.
     *
     * Null keeps the page background, which is right for a screen that frames no single part
     * of the Camara — the Home, which is all of them. On a feature screen the colour has to
     * reach the bar: a green block under a cream bar is a band that stops halfway down.
     */
    area: MagnaArea? = null,
) {
    MediumTopAppBar(
        scrollBehavior = scrollBehavior,
        actions = actions,
        // The title used to be primary green and Bold, which is exactly what a section header
        // is — and MagnaArea.DEPUTADOS.accent is that same green, so the Home's bar and its
        // first section were the same object at two sizes. The bar says where you are; a
        // section says which part of the Camara it is and keeps the accent.
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = area?.container ?: MaterialTheme.colorScheme.background,
            navigationIconContentColor = area?.onContainer ?: MaterialTheme.colorScheme.primary,
            titleContentColor = area?.onContainer ?: MaterialTheme.colorScheme.onSurface,
            scrolledContainerColor = area?.container ?: MaterialTheme.colorScheme.background,
        ),
        navigationIcon = {
            when {
                leftIcon != null -> IconButton(onClick = leftIconClick) {
                    Icon(
                        painter = leftIcon,
                        contentDescription = leftIconContentDescription,
                    )
                }

                // Aligned by hand to the title below it: an IconButton carries its own 16dp of
                // inset, a Text does not.
                navigationLabel != null -> Text(
                    modifier = Modifier.padding(start = WORDMARK_START_PADDING),
                    text = navigationLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = WORDMARK_TRACKING,
                    ),
                )
            }
        },
        title = {
            val expandedStyle =
                if (leftIcon != null) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.headlineLarge

            // MediumTopAppBar interpolates its own title between an expanded and a collapsed
            // style as the bar shrinks, and passing a fixed style into the slot opted out of
            // that: the title stayed at 32sp inside a 64dp row, crowding the wordmark and the
            // action beside it. collapsedFraction runs 0 to 1 as the bar closes.
            val fraction = scrollBehavior?.state?.collapsedFraction ?: 0f
            val style = lerp(expandedStyle, MaterialTheme.typography.titleLarge, fraction)

            Text(
                text = titleText,
                // Light, against the section headers' Bold. The family ships ExtraLight
                // through Bold and the app was rendering Bold in both roles, so the whole
                // lower half of the range was sitting unused while the two collided.
                style = style.copy(fontWeight = FontWeight.Light),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

/**
 * Lines the wordmark's ink up with the title's on the row below.
 *
 * Measured rather than derived, and optical rather than geometric: TopAppBarLayout insets the
 * navigation slot on its own, and the two strings are set at different sizes, so their left
 * side bearings differ. On a 420dpi screen 16dp put the M five pixels right of the 5 and 12dp
 * put it five pixels left; this lands them on the same column.
 */
private val WORDMARK_START_PADDING = 14.dp

/** Enough to read as a mark rather than as a word someone left there. */
private val WORDMARK_TRACKING = 1.sp
