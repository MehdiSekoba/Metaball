package ir.mehdisekoba.sample.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.mehdisekoba.metaball.scroll.MetaballScrollState
import ir.mehdisekoba.sample.ui.data.Palette

private const val HEADER_FADE_START = 0.55f
private const val HEADER_FADE_END = 1f
private val HeaderBarHeight = 160.dp

@Composable
internal fun MetaballHeader(
    scroll: MetaballScrollState,
    avatarCollapse: Float,
    topInset: Dp,
    palette: Palette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(topInset + HeaderBarHeight)
            .graphicsLayer {
                val start = avatarCollapse * HEADER_FADE_START
                val end = avatarCollapse * HEADER_FADE_END
                alpha = if (end <= start) 0f else ((scroll.scrollY - start) / (end - start)).coerceIn(0f, 1f)
            }
            .background(
                Brush.verticalGradient(
                    0f to palette.background,
                    0.65f to palette.background.copy(alpha = 0.85f),
                    1f to palette.background.copy(alpha = 0f),
                )
            )
    )
}
