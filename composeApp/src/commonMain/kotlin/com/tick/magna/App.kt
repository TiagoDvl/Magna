package com.tick.magna

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.CompositionLocalProvider
import com.tick.magna.ui.core.navigation.LocalAnimatedVisibilityScope
import com.tick.magna.ui.core.navigation.LocalSharedTransitionScope
import com.tick.magna.ui.core.navigation.Transicoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.tick.magna.data.analytics.AnalyticsEvent
import com.tick.magna.data.analytics.AnalyticsInterface
import com.tick.magna.data.analytics.toScreenName
import com.tick.magna.features.comissoes.permanentes.detail.ComissaoPermanenteDetailArgs
import com.tick.magna.features.comissoes.permanentes.detail.ComissaoPermanenteDetailScreen
import com.tick.magna.features.deputados.details.DeputadoDetailScreen
import com.tick.magna.features.deputados.details.DeputadoDetailsArgs
import com.tick.magna.features.deputados.search.DeputadosSearchArgs
import com.tick.magna.features.deputados.search.DeputadosSearchScreen
import com.tick.magna.features.home.HomeArgs
import com.tick.magna.features.home.MagnaHomeScreen
import com.tick.magna.features.partidos.details.PartidoDetailsArgs
import com.tick.magna.features.partidos.details.PartidoDetailsScreen
import com.tick.magna.features.comissoes.permanentes.list.ComissoesListArgs
import com.tick.magna.features.comissoes.permanentes.list.ComissoesListScreen
import com.tick.magna.features.partidos.list.PartidosListArgs
import com.tick.magna.features.partidos.list.PartidosListScreen
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsArgs
import com.tick.magna.features.proposicoes.list.ProposicoesListArgs
import com.tick.magna.features.proposicoes.list.ProposicoesListScreen
import com.tick.magna.features.votacoes.detail.VotacaoDetailArgs
import com.tick.magna.features.votacoes.detail.VotacaoDetailScreen
import com.tick.magna.features.proposicoes.details.ProposicaoDetailsScreen
import com.tick.magna.ui.core.theme.MagnaTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    val analytics: AnalyticsInterface = koinInject()

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }

    // Single source of screen tracking. Routes carry argument placeholders rather than
    // real ids, so nothing identifying is reported. Firebase's own automatic screen
    // reporting is disabled in the manifest so this is not counted twice.
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            backStackEntry.destination.route?.let { route ->
                analytics.track(AnalyticsEvent.ScreenView(route.toScreenName()))
            }
        }
    }

    MagnaTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            // Everything the graph draws happens inside one SharedTransitionLayout, which is
            // what lets an element on one screen become an element on the next rather than
            // two elements that happen to look alike. The scopes it needs go down as locals:
            // see ElementoCompartilhado.
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeArgs,
                        enterTransition = Transicoes.entrar,
                        exitTransition = Transicoes.sair,
                        popEnterTransition = Transicoes.voltarEntrando,
                        popExitTransition = Transicoes.voltarSaindo,
                    ) {

                composable<HomeArgs> {
                    Animado { MagnaHomeScreen(navController = navController) }
                }

                composable<DeputadosSearchArgs> {
                    Animado { DeputadosSearchScreen(navController = navController) }
                }

                composable<DeputadoDetailsArgs> {
                    Animado { DeputadoDetailScreen(navController = navController) }
                }

                composable<ComissaoPermanenteDetailArgs> {
                    val args = it.toRoute<ComissaoPermanenteDetailArgs>()

                    Animado {
                        ComissaoPermanenteDetailScreen(
                            viewModel = koinViewModel { parametersOf(args.comissaoPermanenteId) },
                            navController = navController
                        )
                    }
                }

                composable<ComissoesListArgs> {
                    Animado {
                        ComissoesListScreen(
                            navController = navController,
                            onComissaoClick = {
                                navController.navigate(ComissaoPermanenteDetailArgs(it))
                            },
                        )
                    }
                }

                composable<PartidosListArgs> {
                    Animado {
                        PartidosListScreen(
                            navController = navController,
                            onPartidoClick = { navController.navigate(PartidoDetailsArgs(it)) },
                        )
                    }
                }

                composable<PartidoDetailsArgs> {
                    Animado { PartidoDetailsScreen(navController = navController) }
                }

                composable<VotacaoDetailArgs> {
                    Animado { VotacaoDetailScreen(navController = navController) }
                }

                composable<ProposicoesListArgs> {
                    Animado { ProposicoesListScreen(navController = navController) }
                }

                composable<ProposicaoDetailsArgs> {
                    Animado { ProposicaoDetailsScreen(navController = navController) }
                }
                    }
                }
            }

        }
    }
}

/**
 * One destination's body, with the scope a shared element needs to know which screen it is on.
 *
 * `composable {}` hands that scope in as the receiver of its content lambda, and a screen eight
 * composables deep cannot reach a receiver. Providing it here means a component asks for it by
 * local instead, and no screen signature mentions animation at all.
 */
@Composable
private fun AnimatedContentScope.Animado(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
        content()
    }
}