package com.tick.magna.features.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.tick.magna.ui.avatar.Avatar
import com.tick.magna.ui.button.CtaButton
import com.tick.magna.ui.list.ListItem
import com.tick.magna.ui.text.BaseText
import com.tick.magna.ui.text.BaseTextType
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MagnaHome(
    modifier: Modifier = Modifier,
) {
    val fakeNames = rememberSaveable {
        listOf(
            "Elara Vance",
            "Kaelen Rhys",
            "Seraphina Moon",
            "Jasper Thorne",
            "Lyra Swift",
            "Orion Drake",
            "Aria Finch",
            "Ronan Grey",
            "Celeste Evergreen",
            "Finnian Wilde",
            "Nova Sterling",
            "Elias Cole",
            "Wren Holloway",
            "Silas Adler",
            "Genevieve Pine",
            "Asher Blaze",
            "Isla Brooks",
            "Corbin Vale",
            "Aurora Hayes",
            "Declan Reed"
        )
    }

    Scaffold(
        modifier = modifier
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(fakeNames) { name ->
                ListItem(
                    modifier = Modifier.fillMaxWidth(),
                    leftIcon = { Avatar(label = name) },
                    rightIcon = {
                        CtaButton(
                            icon = painterResource(Res.drawable.ic_chevron_right),
                            onClick = {}
                        )
                    },
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BaseText(
                                text = name,
                                type = BaseTextType.TITLE
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