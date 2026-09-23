package com.tick.magna.features.santinho

import magna.composeapp.generated.resources.santinho_nao_mostrado
import magna.composeapp.generated.resources.santinho_mostrar_descricao
import magna.composeapp.generated.resources.santinho_mostrar_titulo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.santinho.CargoDaUrna
import com.tick.magna.data.santinho.Confirmacao
import com.tick.magna.data.santinho.lembrarConfirmacaoDeIdentidade
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import kotlinx.coroutines.launch
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.santinho_apagar
import magna.composeapp.generated.resources.santinho_apagar_confirmacao
import magna.composeapp.generated.resources.santinho_apagar_descricao
import magna.composeapp.generated.resources.santinho_apagar_sim
import magna.composeapp.generated.resources.santinho_atualizado
import magna.composeapp.generated.resources.santinho_aviso_relacao
import magna.composeapp.generated.resources.santinho_cancelar
import magna.composeapp.generated.resources.santinho_cargo_deputado_estadual
import magna.composeapp.generated.resources.santinho_cargo_deputado_federal
import magna.composeapp.generated.resources.santinho_cargo_governador
import magna.composeapp.generated.resources.santinho_cargo_presidente
import magna.composeapp.generated.resources.santinho_cargo_segundo_senador
import magna.composeapp.generated.resources.santinho_cargo_senador
import magna.composeapp.generated.resources.santinho_confirmar_botao
import magna.composeapp.generated.resources.santinho_confirmar_descricao
import magna.composeapp.generated.resources.santinho_confirmar_titulo
import magna.composeapp.generated.resources.santinho_digitos
import magna.composeapp.generated.resources.santinho_esconder
import magna.composeapp.generated.resources.santinho_explicacao
import magna.composeapp.generated.resources.santinho_falhou
import magna.composeapp.generated.resources.santinho_guardado
import magna.composeapp.generated.resources.santinho_mostrar
import magna.composeapp.generated.resources.santinho_recusado
import magna.composeapp.generated.resources.santinho_salvar
import magna.composeapp.generated.resources.santinho_titulo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SantinhoScreen(
    viewModel: SantinhoViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Resolved here rather than injected: a system prompt needs the activity on screen, and
    // keeping one of those in the DI graph is a leak, not a design.
    val confirmacao = lembrarConfirmacaoDeIdentidade()
    val escopo = rememberCoroutineScope()

    val tituloDaConfirmacao = stringResource(Res.string.santinho_confirmar_titulo)
    val descricaoDaConfirmacao = stringResource(Res.string.santinho_confirmar_descricao)
    val botaoDaConfirmacao = stringResource(Res.string.santinho_confirmar_botao)
    val tituloDeMostrar = stringResource(Res.string.santinho_mostrar_titulo)
    val descricaoDeMostrar = stringResource(Res.string.santinho_mostrar_descricao)

    SantinhoContent(
        state = state,
        navigateBack = { navController.popBackStack() },
        onCampoAlterado = viewModel::onCampoAlterado,
        onVisibilidadeAlternada = {
            // Hiding is free; showing is not. The numbers are the thing worth guarding, so the
            // check belongs on the way in rather than only on the way out.
            if (state.revelado) {
                viewModel.onVisibilidadeAlterada(revelado = false)
            } else {
                escopo.launch {
                    when (
                        confirmacao.confirmar(
                            titulo = tituloDeMostrar,
                            descricao = descricaoDeMostrar,
                            botao = botaoDaConfirmacao,
                        )
                    ) {
                        Confirmacao.CONFIRMADA, Confirmacao.INDISPONIVEL ->
                            viewModel.onVisibilidadeAlterada(revelado = true)

                        Confirmacao.RECUSADA -> viewModel.onMostrarRecusado()
                    }
                }
            }
        },
        onApagar = viewModel::onApagar,
        onAvisoVisto = viewModel::onAvisoVisto,
        onGuardar = {
            escopo.launch {
                when (
                    confirmacao.confirmar(
                        titulo = tituloDaConfirmacao,
                        descricao = descricaoDaConfirmacao,
                        botao = botaoDaConfirmacao,
                    )
                ) {
                    // Nothing to ask with is not a refusal. See Confirmacao.INDISPONIVEL.
                    Confirmacao.CONFIRMADA, Confirmacao.INDISPONIVEL -> viewModel.onConfirmado()
                    Confirmacao.RECUSADA -> viewModel.onRecusado()
                }
            }
        },
    )
}

/**
 * Somebody's own note, made to look like the piece of paper it replaces.
 *
 * **It borrows the ballot's shape on purpose.** One box per digit, in the order the machine
 * asks, because the thing being replaced is a scrap of paper somebody copies from while a
 * queue waits behind them. A tidy list of labelled fields would be easier to build and slower
 * to read in the one moment it exists for.
 *
 * **Nothing here is checked against anything.** This app holds a list of federal deputies and
 * nothing else on the ballot, and looking a number up against the only list it happens to have
 * would quietly turn a private note into a record of who somebody is voting for. The digits go
 * in as typed and come out as typed.
 */
@Composable
private fun SantinhoContent(
    modifier: Modifier = Modifier,
    state: SantinhoState,
    navigateBack: () -> Unit = {},
    onCampoAlterado: (CargoDaUrna, String) -> Unit = { _, _ -> },
    onVisibilidadeAlternada: () -> Unit = {},
    onGuardar: () -> Unit = {},
    onApagar: () -> Unit = {},
    onAvisoVisto: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    var confirmandoApagar by remember { mutableStateOf(false) }

    // Long enough to read a sentence, short enough not to sit under the button somebody is
    // about to press again.
    state.aviso?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(DURACAO_DO_AVISO_MS)
            onAvisoVisto()
        }
    }

    MagnaScreen(
        modifier = modifier,
        title = stringResource(Res.string.santinho_titulo),
        navigateBack = navigateBack,
        actions = {
            IconButton(onClick = onVisibilidadeAlternada) {
                Icon(
                    imageVector = if (state.revelado) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = stringResource(
                        if (state.revelado) Res.string.santinho_esconder
                        else Res.string.santinho_mostrar
                    ),
                )
            }
        },
    ) { paddingValues ->
        if (state.carregando) {
            LoadingComponent(modifier = Modifier.fillMaxSize().padding(paddingValues))
            return@MagnaScreen
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimensions.grid16),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16),
        ) {
            Promessa()

            CargoDaUrna.entries.forEach { cargo ->
                CampoDaUrna(
                    cargo = cargo,
                    valor = state.santinho.valorDe(cargo),
                    revelado = state.revelado,
                    onValorAlterado = { onCampoAlterado(cargo, it) },
                )
            }

            Text(
                text = stringResource(Res.string.santinho_aviso_relacao),
                style = typography.bodySmall.copy(color = colorScheme.onSurfaceVariant),
            )

            state.aviso?.let { aviso ->
                Text(
                    text = stringResource(
                        when (aviso) {
                            AvisoDoSantinho.GUARDADO -> Res.string.santinho_guardado
                            AvisoDoSantinho.RECUSADO -> Res.string.santinho_recusado
                            AvisoDoSantinho.NAO_MOSTRADO -> Res.string.santinho_nao_mostrado
                            AvisoDoSantinho.FALHOU -> Res.string.santinho_falhou
                        }
                    ),
                    style = typography.bodyMedium.copy(
                        color = if (aviso == AvisoDoSantinho.GUARDADO) {
                            colorScheme.primary
                        } else {
                            colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onGuardar,
                enabled = state.alterado,
            ) {
                Text(text = stringResource(Res.string.santinho_salvar))
            }

            if (!state.guardado.vazio) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmandoApagar = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error),
                ) {
                    Icon(
                        modifier = Modifier.size(dimensions.grid20),
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(dimensions.grid8))
                    Text(text = stringResource(Res.string.santinho_apagar))
                }
            }

            Spacer(Modifier.height(dimensions.grid24))
        }
    }

    if (confirmandoApagar) {
        AlertDialog(
            onDismissRequest = { confirmandoApagar = false },
            title = { Text(stringResource(Res.string.santinho_apagar_confirmacao)) },
            text = { Text(stringResource(Res.string.santinho_apagar_descricao)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoApagar = false
                        onApagar()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colorScheme.error),
                ) {
                    Text(stringResource(Res.string.santinho_apagar_sim))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoApagar = false }) {
                    Text(stringResource(Res.string.santinho_cancelar))
                }
            },
        )
    }
}

/**
 * What the app promises about this screen, said before anything is typed rather than buried in
 * a settings page nobody opens.
 *
 * Three claims, each of which is enforced a few files away rather than merely stated here: the
 * numbers stay on this device, they are stored encrypted with a key the app cannot read, and
 * nothing about them is ever sent anywhere.
 */
@Composable
private fun Promessa() {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorScheme.surfaceContainer,
                shape = RoundedCornerShape(dimensions.grid12),
            )
            .padding(dimensions.grid16),
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            modifier = Modifier.size(dimensions.grid24),
            imageVector = Icons.Outlined.Lock,
            tint = colorScheme.primary,
            contentDescription = null,
        )

        Text(
            text = stringResource(Res.string.santinho_explicacao),
            style = typography.bodyMedium.copy(color = colorScheme.onSurface),
        )
    }
}

/**
 * One office, drawn as the row of boxes the machine shows.
 *
 * A single text field behind a row of drawn boxes, rather than one field per digit: separate
 * fields mean focus jumping, backspace that does not go back, and a paste that lands in one
 * box. The boxes are the picture; the typing is one field.
 */
@Composable
private fun CampoDaUrna(
    cargo: CargoDaUrna,
    valor: String,
    revelado: Boolean,
    onValorAlterado: (String) -> Unit,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(dimensions.grid8)) {
        Text(
            text = stringResource(cargo.rotulo),
            style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = valor,
            onValueChange = onValorAlterado,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = ESPACAMENTO_DOS_DIGITOS,
                color = if (revelado) colorScheme.onSurface else colorScheme.onSurface,
            ),
            visualTransformation = if (revelado) {
                androidx.compose.ui.text.input.VisualTransformation.None
            } else {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            },
            placeholder = {
                Text(
                    text = stringResource(Res.string.santinho_digitos, cargo.digitos),
                    style = typography.bodyMedium.copy(color = colorScheme.onSurfaceVariant),
                )
            },
            supportingText = {
                Text(
                    text = "${valor.length}/${cargo.digitos}",
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }
}

private val CargoDaUrna.rotulo: StringResource
    get() = when (this) {
        CargoDaUrna.DEPUTADO_FEDERAL -> Res.string.santinho_cargo_deputado_federal
        CargoDaUrna.DEPUTADO_ESTADUAL -> Res.string.santinho_cargo_deputado_estadual
        CargoDaUrna.SENADOR -> Res.string.santinho_cargo_senador
        CargoDaUrna.SEGUNDO_SENADOR -> Res.string.santinho_cargo_segundo_senador
        CargoDaUrna.GOVERNADOR -> Res.string.santinho_cargo_governador
        CargoDaUrna.PRESIDENTE -> Res.string.santinho_cargo_presidente
    }

/** Wide enough that four digits read as four digits rather than as a number. */
private val ESPACAMENTO_DOS_DIGITOS = 8.sp

private const val DURACAO_DO_AVISO_MS = 3500L
