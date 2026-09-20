package com.tick.magna.features.deputados.details

import com.tick.magna.ui.core.navigation.ChaveCompartilhada

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.domain.VotoDeputado
import com.tick.magna.data.domain.deputadoDetailMock
import com.tick.magna.data.domain.deputadoExpensesMock
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.data.source.local.mapper.toDisplayDate
import com.tick.magna.features.votacoes.detail.VotacaoDetailArgs
import com.tick.magna.ui.component.EmptyComponent
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.component.MagnaScreen
import com.tick.magna.ui.component.ProposicaoTipoBadge
import com.tick.magna.ui.component.VotoTag
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.avatar.AvatarSize
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaArea
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.theme.accent
import com.tick.magna.ui.core.theme.container
import com.tick.magna.ui.core.theme.magnaCardElevation
import com.tick.magna.ui.core.theme.onContainer
import com.tick.magna.util.appDeRedeSocial
import com.tick.magna.util.toBrlString
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.deputado_details_expenses_empty
import magna.composeapp.generated.resources.deputado_details_expenses_error
import magna.composeapp.generated.resources.deputado_gabinete_abrir_mapa
import magna.composeapp.generated.resources.deputado_gabinete_anexo_sala
import magna.composeapp.generated.resources.deputado_gabinete_email
import magna.composeapp.generated.resources.deputado_gabinete_enviar_email
import magna.composeapp.generated.resources.deputado_gabinete_label
import magna.composeapp.generated.resources.deputado_gabinete_ligar
import magna.composeapp.generated.resources.deputado_gabinete_sala_apenas
import magna.composeapp.generated.resources.deputado_gabinete_telefone
import magna.composeapp.generated.resources.deputado_tab_despesas
import magna.composeapp.generated.resources.deputado_tab_votos
import magna.composeapp.generated.resources.deputado_votos_empty
import magna.composeapp.generated.resources.deputado_votos_empty_description
import magna.composeapp.generated.resources.deputado_votos_nota
import magna.composeapp.generated.resources.votos_resumo
import magna.composeapp.generated.resources.folder_eye
import magna.composeapp.generated.resources.ic_chevron_right
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoDetailScreen(
    viewModel: DeputadoDetailsViewModel = koinViewModel(),
    navController: NavController
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DeputadoDetails(
        state = state,
        navigateBack = { navController.popBackStack() },
        onExpenseOpened = viewModel::onExpenseOpened,
        onExpenseDocumentOpened = viewModel::onExpenseDocumentOpened,
        onSocialOpened = viewModel::onSocialOpened,
        onTabSelected = viewModel::onTabSelected,
        onVotacaoClick = { id -> navController.navigate(VotacaoDetailArgs(id)) },
    )
}

/**
 * One deputado: who they are, what they spent and how they voted.
 *
 * On [MagnaScreen] like the rest of the app, which is what gives the bar the area's colour and
 * the collapse-on-scroll every other screen already had. It used to be a `BottomSheetScaffold`
 * assembled here with a bare top bar, and the sheet that scaffold existed for is a
 * `ModalBottomSheet` now — the same one the filter sheets use.
 *
 * The header is the area's container with a rounded bottom, the same block the search screen
 * puts its controls in, so the colour runs from the bar into the page instead of stopping at
 * the bar.
 */
@Composable
private fun DeputadoDetails(
    state: DeputadoDetailsState,
    navigateBack: () -> Unit = {},
    onExpenseOpened: (DeputadoExpense) -> Unit = {},
    onExpenseDocumentOpened: () -> Unit = {},
    onSocialOpened: () -> Unit = {},
    onTabSelected: (DeputadoTab) -> Unit = {},
    onVotacaoClick: (String) -> Unit = {},
) {
    var despesaAberta by remember { mutableStateOf<DeputadoExpense?>(null) }

    MagnaScreen(
        title = state.deputado?.name.orEmpty(),
        navigateBack = navigateBack,
        area = MagnaArea.DEPUTADOS,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            DetailHeader(
                deputado = state.deputado,
                detailsState = state.detailsState,
                onSocialOpened = onSocialOpened,
            )

            // Tabs rather than two sections stacked, because each of these owns a
            // LazyColumn and putting both in one Column makes them fight for the height
            // that is left.
            //
            // They are also what names the two lists. The lists used to repeat the name in a
            // titleLarge heading right under the tab that had just said it.
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                DeputadoTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(text = stringResource(tab.label)) },
                    )
                }
            }

            when (state.selectedTab) {
                DeputadoTab.DESPESAS -> DeputadoExpenses(
                    state = state.expensesState,
                    onExpenseClick = { expense ->
                        onExpenseOpened(expense)
                        despesaAberta = expense
                    },
                )

                DeputadoTab.VOTOS -> DeputadoVotos(
                    state = state.votosState,
                    onVotacaoClick = onVotacaoClick,
                )
            }
        }
    }

    despesaAberta?.let { despesa ->
        DeputadoExpenseSheet(
            deputadoExpense = despesa,
            onDismiss = { despesaAberta = null },
            onDocumentOpened = onExpenseDocumentOpened,
        )
    }
}

/**
 * Who this is and how to reach them, in one block of the area's colour.
 *
 * Four identical rows of `bodySmall` was the register's shape printed out: predio, sala,
 * telefone, email, each the same size and none of them doing anything. It is a contact card
 * now — a caption, an address, and two lines under it — and the three that lead somewhere
 * lead there. Nothing is decorated to say so: the icon already names the row and the ripple
 * answers the tap.
 */
@Composable
private fun DetailHeader(
    modifier: Modifier = Modifier,
    deputado: Deputado?,
    detailsState: DetailsState,
    onSocialOpened: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MagnaArea.DEPUTADOS.container,
                shape = RoundedCornerShape(
                    bottomStart = dimensions.grid20,
                    bottomEnd = dimensions.grid20,
                ),
            )
            .padding(dimensions.grid16),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid12),
            verticalAlignment = Alignment.Top,
        ) {
            Avatar(
                photoUrl = deputado?.profilePicture,
                size = AvatarSize.BIG,
                shape = ShapeDefaults.Medium,
                placeholder = painterResource(Res.drawable.ic_light_users),
                // The far end of the flight that starts on whichever list the person was
                // looking at. Keyed by the deputado, not by the screen, so every list that
                // shows this face hands it to this one.
                chaveCompartilhada = deputado?.id?.let(ChaveCompartilhada::fotoDoDeputado),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
            ) {
                deputado?.let { DeputadoMetadata(deputado = it) }

                GabineteDetails(detailsState = detailsState)
            }
        }

        // Below the two columns rather than inside the right one: a chip wrapping in a
        // 200dp column is a chip per line.
        val socials = (detailsState as? DetailsState.Content)?.deputadoDetails?.socials.orEmpty()
        if (socials.isNotEmpty()) {
            SocialsDetails(socials = socials, onSocialOpened = onSocialOpened)
        }
    }
}

/**
 * The state and the party, spaced out.
 *
 * `labelMedium` with a wide tracking rather than the `labelSmall` the list rows use: on a list
 * this is one row's metadata among forty, and here it is the only thing on the screen naming
 * where the person is from.
 */
@Composable
private fun DeputadoMetadata(deputado: Deputado) {
    val metadata = listOfNotNull(deputado.uf, deputado.partido).joinToString(" · ")
    if (metadata.isEmpty()) return

    Text(
        text = metadata,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MagnaArea.DEPUTADOS.onContainer,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = TRACKING,
        ),
    )
}

@Composable
private fun GabineteDetails(
    modifier: Modifier = Modifier,
    detailsState: DetailsState,
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val uriHandler = LocalUriHandler.current
    val naCor = MagnaArea.DEPUTADOS.onContainer

    when (detailsState) {
        DetailsState.Loading -> LoadingComponent(
            modifier = modifier.fillMaxWidth().height(GABINETE_HEIGHT),
        )

        DetailsState.Error -> Unit

        is DetailsState.Content -> {
            val detalhes = detailsState.deputadoDetails
            val sala = salaDoGabinete(detalhes.gabineteBuilding, detalhes.gabineteRoom)
            val telefone = telefoneLegivel(detalhes.gabineteTelephone)
            val email = detalhes.gabineteEmail?.trim()?.takeIf { it.isNotEmpty() }

            if (sala == null && telefone == null && email == null) return

            Column(modifier = modifier) {
                Text(
                    modifier = Modifier.padding(bottom = dimensions.grid2),
                    text = stringResource(Res.string.deputado_gabinete_label),
                    style = typography.labelSmall.copy(color = naCor),
                )

                sala?.let { endereco ->
                    val mapa = mapaDoGabinete(endereco.anexo)

                    GabineteLinha(
                        icon = Icons.Outlined.Apartment,
                        // The room on its own when the annex is not a number. The register
                        // holds an `x` for one gabinete in twenty, and "Anexo x" names nothing.
                        texto = if (endereco.anexo != null) {
                            stringResource(
                                Res.string.deputado_gabinete_anexo_sala,
                                endereco.anexo,
                                endereco.sala,
                            )
                        } else {
                            stringResource(Res.string.deputado_gabinete_sala_apenas, endereco.sala)
                        },
                        estilo = typography.bodyMedium.copy(
                            color = naCor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        acaoLabel = stringResource(Res.string.deputado_gabinete_abrir_mapa),
                        onClick = mapa?.let { url -> { uriHandler.openUri(url) } },
                    )
                }

                telefone?.let { numero ->
                    val discagem = telefoneDiscavel(detalhes.gabineteTelephone)

                    GabineteLinha(
                        icon = Icons.Outlined.Call,
                        texto = numero,
                        estilo = typography.bodySmall.copy(color = naCor),
                        acaoLabel = if (discagem != null) {
                            stringResource(Res.string.deputado_gabinete_ligar)
                        } else {
                            stringResource(Res.string.deputado_gabinete_telefone)
                        },
                        onClick = discagem?.let { url -> { uriHandler.openUri(url) } },
                    )
                }

                email?.let { endereco ->
                    val mailto = emailDiscavel(endereco)

                    GabineteLinha(
                        icon = Icons.Outlined.MailOutline,
                        texto = endereco,
                        estilo = typography.bodySmall.copy(color = naCor),
                        acaoLabel = if (mailto != null) {
                            stringResource(Res.string.deputado_gabinete_enviar_email)
                        } else {
                            stringResource(Res.string.deputado_gabinete_email)
                        },
                        onClick = mailto?.let { url -> { uriHandler.openUri(url) } },
                    )
                }
            }
        }
    }
}

/**
 * One line of the gabinete, which may or may not lead somewhere.
 *
 * Emoji used to stand in for these icons. They render differently on every device, they are
 * announced out loud by a screen reader as their own name, and "building, door, telephone,
 * envelope" is not what a gabinete is.
 *
 * A line with no [onClick] is still a line: a telephone the register wrote in a shape nothing
 * can dial is worth reading and not worth tapping, and the difference shows up as the absence
 * of a ripple rather than as a row that looks disabled.
 */
@Composable
private fun GabineteLinha(
    icon: ImageVector,
    texto: String,
    estilo: TextStyle,
    acaoLabel: String,
    onClick: (() -> Unit)?,
) {
    val dimensions = LocalDimensions.current

    Row(
        // Full width and padded rather than wrapped tight: the text of a phone number is about
        // 24dp tall, and a target that size is one somebody misses. It is still short of the
        // 48 an isolated control owes — three rows of that would be a header and a half — so
        // the compromise is stated rather than hidden: ~32dp, and the whole row is the target
        // rather than the glyphs.
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraSmall)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = acaoLabel, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(vertical = dimensions.grid4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
    ) {
        Icon(
            modifier = Modifier.size(dimensions.grid16),
            imageVector = icon,
            tint = estilo.color,
            // The label carries the meaning for a screen reader; the icon repeating it would
            // make every line of the gabinete read twice.
            contentDescription = acaoLabel,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = texto,
            style = estilo,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // The only thing that says the row leads somewhere, and the same glyph every list in
        // the app uses for that — smaller, and in the accent rather than the text colour, so
        // it sits behind the address instead of competing with it. Green on the area's own
        // container measures 3.33:1 in light and 4.18 in dark, which is what an affordance
        // owes. A row with nothing to open draws none, so the difference is visible before
        // anybody taps.
        if (onClick != null) {
            Icon(
                modifier = Modifier.size(CHEVRON_GABINETE),
                painter = painterResource(Res.drawable.ic_chevron_right),
                tint = MagnaArea.DEPUTADOS.accent,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun SocialsDetails(
    modifier: Modifier = Modifier,
    socials: Map<String, String>,
    onSocialOpened: () -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val uriHandler = LocalUriHandler.current

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
    ) {
        socials.entries.forEach { entry ->
            AssistChip(
                // The app's own surface on the area's container, like the filter chips on the
                // search screen. A default chip here is a transparent outline on green.
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = colorScheme.surface,
                    labelColor = colorScheme.onSurface,
                ),
                border = null,
                label = {
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                onClick = {
                    onSocialOpened()
                    abrirRedeSocial(uriHandler, entry.value)
                }
            )
        }
    }
}

/**
 * Opens the profile in the app that owns it, and in the browser when there is no such app.
 *
 * Every link the register holds is an `https://` one, so this used to hand a signed-in
 * reader's own profile to a browser. The scheme is tried first and the web link is the
 * fallback, in that order, because there is no way to ask what is installed that Android 11's
 * package visibility does not also have to be told about in advance — and a launch that finds
 * nothing throws, which is the answer.
 *
 * The catch is deliberately wide: the exception a missing handler produces is
 * `ActivityNotFoundException` on Android and something else on every other target, and all of
 * them mean the same thing here.
 */
private fun abrirRedeSocial(uriHandler: UriHandler, url: String) {
    val app = appDeRedeSocial(url)

    if (app != null) {
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        try {
            uriHandler.openUri(app)
            return
        } catch (naoInstalado: Exception) {
            // Nothing answers that scheme. The web link below is the whole recovery.
        }
    }

    uriHandler.openUri(url)
}

@Composable
private fun DeputadoExpenses(
    modifier: Modifier = Modifier,
    state: ExpensesState,
    onExpenseClick: (DeputadoExpense) -> Unit = {},
) {
    val dimensions = LocalDimensions.current

    when (state) {
        ExpensesState.Loading -> LoadingComponent(modifier = modifier.fillMaxSize())

        ExpensesState.Empty -> EmptyComponent(
            modifier = modifier.fillMaxSize(),
            title = stringResource(Res.string.deputado_details_expenses_empty),
        )

        ExpensesState.Error -> EmptyComponent(
            modifier = modifier.fillMaxSize(),
            title = stringResource(Res.string.deputado_details_expenses_error),
        )

        is ExpensesState.Content -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = dimensions.grid16,
                vertical = dimensions.grid8,
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            items(state.expenses) { expense ->
                DespesaCard(expense = expense, onClick = { onExpenseClick(expense) })
            }
        }
    }
}

/**
 * One expense, built like the rows on the deputado search.
 *
 * It used to carry three colours that belong to other areas: the type in `secondary`, which is
 * the gold of proposicoes, and the amount and the document icon in `tertiary`, which is the
 * blue of partidos. On a deputado screen the only accent is the green, and most of the row is
 * not an accent at all — a title, a metadata line, and a figure.
 */
@Composable
private fun DespesaCard(expense: DeputadoExpense, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
            ) {
                Text(
                    text = expense.tipoDespesa,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = expense.nomeFornecedor,
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The one figure on the row, in the darker green of the pair rather than
                    // the accent: eleven-point text owes 4.5:1 and the accent measures 4.1 on
                    // this card.
                    Text(
                        text = expense.valorDocumento.toBrlString(),
                        style = typography.labelSmall.copy(
                            color = MagnaArea.DEPUTADOS.onContainer,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        text = "·",
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                    Text(
                        text = expense.dataDocumento,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )

                    if (expense.urlDocumento != null) {
                        Icon(
                            modifier = Modifier.size(dimensions.grid16),
                            painter = painterResource(Res.drawable.folder_eye),
                            tint = MagnaArea.DEPUTADOS.accent,
                            contentDescription = null,
                        )
                    }
                }
            }

            // The same affordance as every other list in the app: 18dp in the area's colour.
            // This one was a different glyph in `outline`.
            Icon(
                modifier = Modifier.size(CHEVRON),
                painter = painterResource(Res.drawable.ic_chevron_right),
                tint = MagnaArea.DEPUTADOS.accent,
                contentDescription = null,
            )
        }
    }
}

private val DeputadoTab.label: StringResource
    get() = when (this) {
        DeputadoTab.DESPESAS -> Res.string.deputado_tab_despesas
        DeputadoTab.VOTOS -> Res.string.deputado_tab_votos
    }

@Composable
private fun DeputadoVotos(state: VotosState, onVotacaoClick: (String) -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    when (state) {
        VotosState.Loading -> LoadingComponent(modifier = Modifier.fillMaxSize())

        VotosState.Error -> EmptyComponent(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(Res.string.deputado_details_expenses_error),
        )

        // The common branch, not the rare one. Only 2% of what the Camara registers is
        // nominal, so a deputado with nothing here is ordinary — and saying why matters,
        // because an empty list otherwise reads as "did not vote".
        VotosState.Empty -> EmptyComponent(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(Res.string.deputado_votos_empty),
            description = stringResource(Res.string.deputado_votos_empty_description),
        )

        is VotosState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = dimensions.grid16,
                vertical = dimensions.grid8,
            ),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            item(key = RESUMO_KEY) {
                Text(
                    modifier = Modifier.padding(bottom = dimensions.grid4),
                    text = stringResource(
                        Res.string.votos_resumo,
                        state.votos.size,
                        state.sim,
                        state.nao,
                        state.outros,
                    ),
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }

            items(state.votos, key = { it.votacaoId }) { voto ->
                VotoCard(voto = voto, onClick = { onVotacaoClick(voto.votacaoId) })
            }

            // What the list does not contain, said once. The window is a quarter of the
            // plenary, so this is not the deputado's whole record.
            item(key = NOTA_KEY) {
                Text(
                    modifier = Modifier.padding(top = dimensions.grid8),
                    text = stringResource(Res.string.deputado_votos_nota),
                    style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}

@Composable
private fun VotoCard(voto: VotoDeputado, onClick: () -> Unit) {
    val dimensions = LocalDimensions.current
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = magnaCardElevation(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(dimensions.grid12),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid8),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.grid8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VotoTag(voto = voto.voto)

                voto.dataHoraRegistro?.let { data ->
                    Text(
                        text = data.toDisplayDate(),
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }

                voto.siglaOrgao?.let { orgao ->
                    Text(
                        text = orgao,
                        style = typography.labelSmall.copy(color = colorScheme.onSurfaceVariant),
                    )
                }
            }

            // The same badge the Home and the proposicoes list draw, rather than a green bold
            // line invented here: a proposition looks like a proposition wherever it appears.
            // The sigla is the first token of the label the sweep built — `PLP 74/2026`.
            voto.proposicaoRotulo?.let { rotulo ->
                ProposicaoTipoBadge(
                    siglaTipo = rotulo.substringBefore(' '),
                    label = rotulo,
                )
            }

            Text(text = voto.descricao, style = typography.bodyMedium)
        }
    }
}

private const val RESUMO_KEY = "resumo"
private const val NOTA_KEY = "nota"

/** An affordance, not a control, at the size every other list in the app uses. */
private val CHEVRON = 18.dp

/** Smaller than a list's, because it sits beside `bodySmall` rather than a card. */
private val CHEVRON_GABINETE = 14.dp

/** Enough not to collapse the header while the gabinete is on its way. */
private val GABINETE_HEIGHT = 120.dp

/** Wide enough to read as spaced out rather than as a typo. */
private val TRACKING = 1.sp

@Preview
@Composable
private fun PreviewDeputadoDetails() {
    MagnaTheme {
        DeputadoDetails(
            state = DeputadoDetailsState(
                deputado = deputadosMock.random(),
                detailsState = DetailsState.Content(deputadoDetailMock),
                expensesState = ExpensesState.Content(deputadoExpensesMock)
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewDeputadoDetailsLoading() {
    MagnaTheme {
        DeputadoDetails(
            state = DeputadoDetailsState(
                deputado = deputadosMock.random(),
                detailsState = DetailsState.Loading,
                expensesState = ExpensesState.Loading
            ),
        )
    }
}
