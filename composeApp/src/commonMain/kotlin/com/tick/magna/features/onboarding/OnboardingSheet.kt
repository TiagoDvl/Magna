package com.tick.magna.features.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tick.magna.ui.core.text.BaseText
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingSheet(
    viewModel: OnboardingViewModel = koinViewModel(),
    closeSheet: () -> Unit = {}
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val sendAction = viewModel::processAction

    var isExpanded by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BaseText(text = "Onboading time")

        ExposedDropdownMenuBox(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            expanded = isExpanded,
            onExpandedChange = {
                isExpanded = !isExpanded
            }
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                value = "Select legislatura period",
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                }
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                state.value.legislaturas.forEachIndexed { index, legislatura ->
                    DropdownMenuItem(
                        text = {
                            Text("${legislatura.startDate}  ~  ${legislatura.endDate}")
                        },
                        onClick = {
                            sendAction(Action.PickLegislaturaPeriod(legislatura.startDate))
                            isExpanded = false
                            closeSheet()
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}