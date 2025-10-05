package com.tick.magna.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.avatar.Avatar
import com.tick.magna.ui.button.CtaButton
import com.tick.magna.ui.list.ListItem
import com.tick.magna.ui.text.BaseText
import com.tick.magna.ui.text.BaseTextType
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MagnaHome(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val deputadosState by viewModel.deputadosListState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { Text("Vish") }
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(deputadosState) { deputado ->
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leftIcon = { Avatar(label = deputado.nome) },
                    rightIcon = {
                        CtaButton(
                            icon = painterResource(Res.drawable.ic_chevron_right),
                            onClick = {}
                        )
                    },
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BaseText(
                                text = deputado.nome,
                                type = BaseTextType.TITLE
                            )
                            BaseText(
                                text = deputado.email,
                                type = BaseTextType.SUBTITLE
                            )
                            BaseText(
                                text = deputado.partido,
                                type = BaseTextType.BODY
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewMagnaHome() {
    MagnaHome()
}