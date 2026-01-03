package com.tick.magna.features.deputados.detail

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
import androidx.compose.ui.text.font.FontWeight
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
import com.tick.magna.ui.core.text.BaseText
import com.tick.magna.ui.core.theme.LocalDimensions
import com.tick.magna.ui.core.theme.MagnaTheme
import com.tick.magna.ui.core.topbar.MagnaTopBar
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
        sheetContainerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MagnaTopBar(
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
                BaseText(
                    text = "\uD83D\uDCCD ${deputado.uf}",
                    style = chipsStyle.copy(fontWeight = FontWeight.Bold)
                )

                BaseText(
                    text = "-",
                    style = chipsStyle
                )

                deputado.partido?.let { partido ->
                    BaseText(
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
                    BaseText(
                        text = stringResource(Res.string.deputado_details_loading_details),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }
            is DetailsState.Content -> {
                GabineteDetails(deputadoDetails = detailsState.deputadoDetails)
                SocialsDetails(socials = detailsState.deputadoDetails.socials.orEmpty())
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
            BaseText(
                text = "\uD83C\uDFE2 $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteRoom?.let {
            BaseText(
                text = "\uD83D\uDEAA $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteTelephone?.let {
            BaseText(
                text = "\uD83D\uDCDE $it",
                style = gabineteDetailsStyle
            )
        }

        deputadoDetails.gabineteEmail?.let {
            BaseText(
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
    socials: List<String>
) {
    val dimensions = LocalDimensions.current
    val chipsStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val mappedSocials = socials.mapNotNull { social ->
        when {
            social.contains("facebook", ignoreCase = true) -> "Facebook"
            social.contains("twitter", ignoreCase = true) -> "Twitter"
            social.contains("instagram", ignoreCase = true) -> "Instagram"
            social.contains("youtube", ignoreCase = true) -> "Youtube"
            else -> null
        }
    }.take(4)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimensions.grid4),
        content = {
            mappedSocials.forEach { text ->
                AssistChip(
                    label = {
                        BaseText(
                            text = text,
                            style = chipsStyle
                        )
                    },
                    onClick = {}
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
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxSize().padding(top = dimensions.grid8),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid20)
    ) {
        BaseText(
            text = stringResource(Res.string.deputado_details_expense_title),
            style = style.titleLarge.copy(
                color = colors.secondary,
                fontWeight = FontWeight.Bold
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
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

                            BaseText(
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClick = {
                                        onExpenseClick(expense)
                                    }
                                ),
                            verticalArrangement = Arrangement.spacedBy(dimensions.grid4)
                        ) {
                            BaseText(text = expense.tipoDespesa, style = style.bodyLarge.copy(color = colors.onSurface))
                            BaseText(
                                text = expense.nomeFornecedor,
                                style = style.bodyMedium.copy(
                                    color = colors.onSurface,
                                    fontWeight = FontWeight.ExtraLight
                                )
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(dimensions.grid2),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BaseText(
                                    text = expense.valorDocumento,
                                    style = style.bodySmall.copy(
                                        color = colors.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                BaseText(text = "-", style = style.bodySmall.copy(color = colors.onSurface))
                                BaseText(text = expense.dataDocumento, style = style.bodySmall.copy(color = colors.onSurface))

                                if (expense.urlDocumento != null) {
                                    Icon(
                                        modifier = Modifier.size(dimensions.grid16),
                                        painter = painterResource(Res.drawable.folder_eye),
                                        contentDescription = null
                                    )
                                }
                            }
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
    val style = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.sizeIn(minHeight = 300.dp).fillMaxWidth().padding(dimensions.grid8),
        verticalArrangement = Arrangement.spacedBy(dimensions.grid16)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BaseText(
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
            onClick = {},
            content = {
                BaseText(
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
        BaseText(
            text = "$title:",
            style = style.bodyMedium.copy(
                color = colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        BaseText(
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