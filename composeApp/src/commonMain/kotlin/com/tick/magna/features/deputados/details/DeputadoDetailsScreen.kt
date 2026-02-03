package com.tick.magna.features.deputados.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.tick.magna.data.domain.Deputado
import com.tick.magna.data.domain.DeputadoDetails
import com.tick.magna.data.domain.DeputadoExpense
import com.tick.magna.data.domain.deputadoDetailMock
import com.tick.magna.data.domain.deputadoExpensesMock
import com.tick.magna.data.domain.deputadosMock
import com.tick.magna.ui.component.LoadingComponent
import com.tick.magna.ui.core.avatar.Avatar
import com.tick.magna.ui.core.avatar.AvatarSize
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaMediumTopBar
import kotlinx.coroutines.launch
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.deputado_details_check_document
import magna.composeapp.generated.resources.deputado_details_expense_despesa_type
import magna.composeapp.generated.resources.deputado_details_expense_document_date
import magna.composeapp.generated.resources.deputado_details_expense_document_number
import magna.composeapp.generated.resources.deputado_details_expense_document_value
import magna.composeapp.generated.resources.deputado_details_expense_month
import magna.composeapp.generated.resources.deputado_details_expense_supplier_cnpjcpf
import magna.composeapp.generated.resources.deputado_details_expense_supplier_name
import magna.composeapp.generated.resources.deputado_details_expense_title
import magna.composeapp.generated.resources.deputado_details_expense_year
import magna.composeapp.generated.resources.deputado_details_loading_details
import magna.composeapp.generated.resources.deputado_details_loading_expenses
import magna.composeapp.generated.resources.folder_eye
import magna.composeapp.generated.resources.ic_arrow_right
import magna.composeapp.generated.resources.ic_chevron_left
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeputadoDetailScreen(
    viewModel: DeputadoDetailsViewModel = koinViewModel(),
    navController: NavController
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    DeputadoDetails(
        state = state.value,
        navigateBack = { navController.popBackStack() }
    )
}

@Composable
private fun DeputadoDetails(
    state: DeputadoDetailsState,
    navigateBack: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    val bottomSheetState: SheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState)
    val scope = rememberCoroutineScope()

    var localSheetState by remember { mutableStateOf<DeputadoDetailsSheetState?>(null) }

    fun showSheet(homeSheetState: DeputadoDetailsSheetState) {
        scope.launch {
            localSheetState = homeSheetState
            bottomSheetState.expand()
        }
    }

    fun hideSheet() {
        scope.launch {
            bottomSheetState.hide()
        }
    }

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        topBar = {
            MagnaMediumTopBar(
                titleText = state.deputado?.name.orEmpty(),
                leftIcon = painterResource(Res.drawable.ic_chevron_left),
                leftIconClick = navigateBack
            )
        },
        scaffoldState = bottomSheetScaffoldState,
        sheetTonalElevation = LocalDimensions.current.grid4,
        sheetShadowElevation = LocalDimensions.current.grid12,
        sheetSwipeEnabled = true,
        sheetContent = {
            when (val state = localSheetState) {
                is DeputadoDetailsSheetState.Expense -> {
                    DeputadoExpenseDetails(
                        deputadoExpense = state.deputadoExpense,
                        onCloseSheet = { hideSheet() }
                    )
                }

                null -> Unit
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DetailHeader(
                modifier = Modifier.padding(LocalDimensions.current.grid8),
                deputado = state.deputado,
                detailsState = state.detailsState
            )

            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = colorScheme.surfaceDim)

            DeputadoExpenses(
                modifier = Modifier.padding(LocalDimensions.current.grid8),
                state = state.expensesState,
                onExpenseClick = {
                    showSheet(DeputadoDetailsSheetState.Expense(it))
                },
            )
        }
    }
}

@Composable
private fun DetailHeader(
    modifier: Modifier = Modifier,
    deputado: Deputado?,
    detailsState: DetailsState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid12),
        verticalAlignment = Alignment.Top
    ) {
        DetailAvatar(deputado = deputado)
        DetailContent(detailsState = detailsState)
    }
}

@Composable
private fun DetailAvatar(
    modifier: Modifier = Modifier,
    deputado: Deputado?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8),
    ) {
        Avatar(
            photoUrl = deputado?.profilePicture,
            size = AvatarSize.BIG,
            shape = ShapeDefaults.Medium,
            placeholder = painterResource(Res.drawable.ic_light_users),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chipsStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
            deputado?.let {
                Text(
                    text = "\uD83D\uDCCD ${deputado.uf}",
                    style = chipsStyle.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "-",
                    style = chipsStyle
                )

                deputado.partido?.let { partido ->
                    Text(
                        text = partido,
                        style = chipsStyle
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    modifier: Modifier = Modifier,
    detailsState: DetailsState
) {
    Column(modifier = modifier) {
        when (detailsState) {
            DetailsState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingComponent(modifier = Modifier.fillMaxWidth().height(160.dp))
                    Text(
                        text = stringResource(Res.string.deputado_details_loading_details),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            is DetailsState.Content -> {
                GabineteDetails(deputadoDetails = detailsState.deputadoDetails)
                SocialsDetails(socials = detailsState.deputadoDetails.socials)
            }
        }
    }
}

@Composable
private fun GabineteDetails(
    modifier: Modifier = Modifier,
    deputadoDetails: DeputadoDetails
) {
    val gabineteDetailsStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LocalDimensions.current.grid4)
    ) {
        deputadoDetails.gabineteBuilding?.let {
            Text(
                text = "\uD83C\uDFE2 $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteRoom?.let {
            Text(
                text = "\uD83D\uDEAA $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteTelephone?.let {
            Text(
                text = "\uD83D\uDCDE $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteEmail?.let {
            Text(
                text = "✉\uFE0F $it",
                style = gabineteDetailsStyle,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SocialsDetails(
    modifier: Modifier = Modifier,
    socials: Map<String, String>
) {
    val dimensions = LocalDimensions.current
    val chipsStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val uriHandler = LocalUriHandler.current


    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
        content = {
            socials.entries.forEach { entry ->
                AssistChip(
                    label = { Text(text = entry.key, style = chipsStyle) },
                    onClick = { uriHandler.openUri(entry.value) }
                )
            }
        }
    )
}

@Composable
fun DeputadoExpenses(
    modifier: Modifier = Modifier,
    state: ExpensesState,
    onExpenseClick: (DeputadoExpense) -> Unit = {},
) {
    val dimensions = LocalDimensions.current
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxSize().padding(top = dimensions.grid8),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid20)
    ) {
        Text(
            text = stringResource(Res.string.deputado_details_expense_title),
            style = typography.titleLarge.copy(
                textAlign = TextAlign.Center,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid24)
        ) {
            when (state) {
                ExpensesState.Loading -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.tertiary
                            )

                            Text(
                                text = stringResource(Res.string.deputado_details_loading_expenses),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            )
                        }
                    }
                }

                is ExpensesState.Content -> {
                    items(state.expenses) { expense ->
                        Row(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .clickable(
                                        onClick = {
                                            onExpenseClick(expense)
                                        }
                                    ),
                                verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                            ) {

                                Text(text = expense.tipoDespesa, style = typography.bodyLarge.copy(color = colorScheme.secondary))
                                Text(
                                    text = expense.nomeFornecedor,
                                    style = typography.bodyMedium.copy(
                                        color = colorScheme.onSurface,
                                        fontWeight = FontWeight.ExtraLight
                                    )
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(dimensions.grid2),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = expense.valorDocumento,
                                        style = typography.bodySmall.copy(
                                            color = colorScheme.tertiary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(text = "-", style = typography.bodySmall.copy(color = colorScheme.onSurface))
                                    Text(text = expense.dataDocumento, style = typography.bodySmall.copy(color = colorScheme.onSurface))

                                    if (expense.urlDocumento != null) {
                                        Icon(
                                            modifier = Modifier.size(dimensions.grid16),
                                            painter = painterResource(Res.drawable.folder_eye),
                                            tint = colorScheme.tertiary,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            Icon(
                                painter = painterResource(Res.drawable.ic_arrow_right),
                                tint = colorScheme.tertiary,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeputadoExpenseDetails(
    deputadoExpense: DeputadoExpense,
    onCloseSheet: () -> Unit = {}
) {
    val dimensions = LocalDimensions.current
    val uriHandler = LocalUriHandler.current
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .sizeIn(minHeight = 300.dp)
            .fillMaxWidth()
            .padding(horizontal = dimensions.grid16)
            .padding(bottom = dimensions.grid24),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(0.80f),
                text = deputadoExpense.tipoDespesa,
                style = style.titleSmall.copy(
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            )

            IconButton(onClick = onCloseSheet) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
        ) {
            ExpenseRow(stringResource(Res.string.deputado_details_expense_year), deputadoExpense.ano.toString())
            ExpenseRow(stringResource(Res.string.deputado_details_expense_month), deputadoExpense.mes.toString())
            ExpenseRow(stringResource(Res.string.deputado_details_expense_despesa_type), deputadoExpense.tipoDespesa)
            ExpenseRow(stringResource(Res.string.deputado_details_expense_document_date), deputadoExpense.dataDocumento)
            ExpenseRow(stringResource(Res.string.deputado_details_expense_document_number), deputadoExpense.numDocumento)
            ExpenseRow(stringResource(Res.string.deputado_details_expense_document_value), deputadoExpense.valorDocumento)
            ExpenseRow(stringResource(Res.string.deputado_details_expense_supplier_name), deputadoExpense.nomeFornecedor)
            ExpenseRow(stringResource(Res.string.deputado_details_expense_supplier_cnpjcpf), deputadoExpense.cnpjCpfFornecedor)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = deputadoExpense.urlDocumento != null,
            onClick = { deputadoExpense.urlDocumento?.let { uriHandler.openUri(it) } },
            content = {
                Text(
                    text = stringResource(Res.string.deputado_details_check_document)
                )
            }
        )

    }
}

@Composable
fun ExpenseRow(
    title: String,
    value: String
) {
    val dimensions = LocalDimensions.current
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensions.grid4),
    ) {
        Text(
            text = "$title:",
            style = style.bodyMedium.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = value,
            style = style.bodyMedium.copy(color = colors.onSurface)
        )
    }
}

@Preview
@Composable
fun PreviewDeputadoDetails() {
    MagnaTheme {
        DeputadoDetails(
            state = DeputadoDetailsState(
                deputado = deputadosMock.random(),
                detailsState = DetailsState.Content(deputadoDetailMock),
                expensesState = ExpensesState.Content(deputadoExpensesMock)
            ),
            navigateBack = {}
        )
    }
}

@Preview
@Composable
fun PreviewDeputadoDetailsLoading() {
    MagnaTheme {
        DeputadoDetails(
            state = DeputadoDetailsState(
                deputado = deputadosMock.random(),
                detailsState = DetailsState.Loading,
                expensesState = ExpensesState.Loading
            ),
            navigateBack = {}
        )
    }
}