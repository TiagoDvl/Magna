package com.tick.magna

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.tick.magna.features.deputados.detail.DeputadoDetailScreen
import com.tick.magna.features.deputados.detail.DeputadoDetailsArgs
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.features.deputados.search.DeputadosSearchScreen
import com.tick.magna.features.home.HomeArgs
import com.tick.magna.features.home.MagnaHomeScreen
import com.tick.magna.ui.core.theme.MagnaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    MagnaTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            NavHost(
                navController = navController,
                startDestination = HomeArgs
            ) {
                composable<HomeArgs> {
                    MagnaHomeScreen(navController = navController)
                }

                composable<DeputadosSearchArgs> {
                    DeputadosSearchScreen(navController = navController)
                }

                composable<DeputadoDetailsArgs> {
                    DeputadoDetailScreen(navController = navController)
                }
            }

        }
    }
}