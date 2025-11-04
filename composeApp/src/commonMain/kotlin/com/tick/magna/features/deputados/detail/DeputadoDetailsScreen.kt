package com.tick.magna.features.deputados.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.button.CtaButton
import com.tick.magna.ui.core.list.ListItem
import com.tick.magna.ui.core.text.BaseText
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoDetailScreen(
    viewModel: DeputadoDetailsViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    when {
        state.value.isLoading -> LoadingComponent()
        else -> {
            state.value.deputado?.let { DeputadoDetails(deputado = it) }
        }
    }
}

@Composable
private fun DeputadoDetails(
    deputado: Deputado
) {
    Scaffold {
        Column {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                leftIcon = {
                    Avatar(photoUrl = deputado.profilePicture)
                },
                rightIcon = {
                    CtaButton(
                        icon = painterResource(Res.drawable.ic_chevron_right),
                        onClick = {  }
                    )
                },
                content = {
                    Column {
                        BaseText(text = deputado.name)
                        BaseText(text = deputado.uf)
                    }
                }
            )
        }
    }
}