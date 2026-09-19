package com.tick.magna.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource

/**
 * The scaffolding every screen of this app shares, so that a new one starts with the identity
 * already on rather than being assembled by hand again.
 *
 * Eight screens were each writing their own `Scaffold` plus `MagnaMediumTopBar`, which is how
 * they drifted: the back callback is called `navigateBack` in some and `onBack` in others, and
 * none of them had any scroll behaviour at all — there was not one `scrollBehavior` or
 * `nestedScroll` in the whole of commonMain.
 *
 * The bar collapses as the content scrolls under it. `enterAlways` rather than
 * `exitUntilCollapsed` because it brings the title back on the first upward scroll instead of
 * making somebody scroll to the top of a long list to see where they are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagnaScreen(
    title: String,
    modifier: Modifier = Modifier,
    /** Null draws no back arrow, for a screen that is not pushed onto anything. */
    navigateBack: (() -> Unit)? = null,
    /** Fills the navigation row of a screen with no back arrow. See MagnaMediumTopBar. */
    navigationLabel: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    /** Sits under the bar and scrolls with it: a tab row, a filter strip. */
    belowTopBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.foundation.layout.Column {
                MagnaMediumTopBar(
                    titleText = title,
                    leftIcon = navigateBack?.let { painterResource(Res.drawable.ic_arrow_back) },
                    leftIconClick = navigateBack ?: {},
                    navigationLabel = navigationLabel,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )

                belowTopBar()
            }
        },
        content = content,
    )
}
