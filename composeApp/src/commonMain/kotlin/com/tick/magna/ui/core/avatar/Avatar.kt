package com.tick.magna.ui.core.avatar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.tick.magna.ui.core.image.MagnaImage
import com.tick.magna.ui.core.navigation.elementoCompartilhado
import com.tick.magna.ui.core.theme.LocalDimensions
import magna.composeapp.generated.resources.Res
import magna.composeapp.generated.resources.ic_light_users
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    photoUrl: String? = null,
    placeholder: Painter? = null,
    shape: Shape = CircleShape,
    /**
     * Null takes the size from [modifier] instead.
     *
     * It used to be `AvatarSize.SMALL` and applied after the caller's modifier, so
     * `Avatar(modifier = Modifier.size(40.dp))` silently drew 32 — every call site that asked
     * for a size was getting the default and no one could see why.
     */
    size: AvatarSize? = AvatarSize.SMALL,
    /**
     * Carries this face across a navigation, to wherever the same key appears next.
     *
     * On the Card rather than on the image inside it, so the shape travels too: the same
     * photograph is a circle in a list and a rounded square at the top of a detail screen, and
     * watching the corners round off on the way is most of what says it is the same picture
     * rather than two of them.
     */
    chaveCompartilhada: String? = null,
    badge: @Composable () -> Unit = {}
) {
    Card(
        modifier = (if (size == null) modifier else modifier.size(size.getSize()))
            .let { if (chaveCompartilhada == null) it else it.elementoCompartilhado(chaveCompartilhada) },
        shape = shape
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            MagnaImage(
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                imageUrl = photoUrl,
                placeholder = placeholder,
                contentDescription = null
            )

            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                badge()
            }
        }
    }
}

enum class AvatarSize {
    SMALL, MEDIUM, BIG;

    @Composable
    fun getSize(): Dp {
        return when (this) {
            SMALL -> LocalDimensions.current.grid32
            MEDIUM -> LocalDimensions.current.grid32 * 2
            BIG -> LocalDimensions.current.grid32 * 4
        }
    }
}

@Preview
@Composable
fun AvatarPreview() {
    Avatar(photoUrl = "")
}

@Preview
@Composable
fun AvatarShapePreview() {
    Avatar(
        photoUrl = "",
        shape = ShapeDefaults.Medium,
        size = AvatarSize.BIG,
        badge = {
            Icon(painter = painterResource(Res.drawable.ic_light_users), contentDescription = null)
        }
    )
}