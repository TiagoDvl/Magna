package com.tick.magna

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.tick.magna.features.intro.IntroArgs
import com.tick.magna.features.intro.IntroScreen
import com.tick.magna.features.welcome.WelcomeArgs
import com.tick.magna.features.welcome.WelcomeScreen
import com.tick.magna.ui.core.theme.MagnaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App(
    viewModel: AppViewModel = koinViewModel()
) {
    val state = viewModel.appState.collectAsStateWithLifecycle()
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
            val startDestination = when (state.value) {
                AppState.Welcome -> WelcomeArgs
                AppState.Home -> HomeArgs
                AppState.Intro -> IntroArgs
            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<IntroArgs> {
                    IntroScreen()
                }

                composable<WelcomeArgs> {
                    WelcomeScreen(navController = navController)
                }

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