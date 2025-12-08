package com.tick.magna.features.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.magna_logo
import magna.composeapp.generated.resources.splash_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SplashScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
            ) {
                Image(
                    painter = painterResource(Res.drawable.magna_logo),
                    contentDescription = null
                )
                BaseText(text = stringResource(Res.string.splash_description), style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.secondary))
            }
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}