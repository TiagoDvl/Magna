package com.tick.magna.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tick.magna.ui.core.text.BaseText
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_light_users
import magna.composeapp.generated.resources.something_went_wrong
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SomethingWentWrongComponent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_light_users),
            tint = MaterialTheme.colorScheme.error,
            contentDescription = null
        )
        BaseText(
            text = stringResource(Res.string.something_went_wrong),
            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.error),
        )
    }
}

@Preview
@Composable
fun SomethingWentWrongComponentPreview() {
    SomethingWentWrongComponent()
}