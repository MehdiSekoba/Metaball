package ir.mehdisekoba.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import ir.mehdisekoba.sample.ui.data.rememberPalette

enum class SkeletonTone { Soft, Strong }

@Composable
fun Skeleton(
    height: Dp,
    modifier: Modifier = Modifier,
    radius: Dp = height / 2,
    tone: SkeletonTone = SkeletonTone.Strong,
) {
    val palette = rememberPalette()
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(if (tone == SkeletonTone.Soft) palette.boneSoft else palette.bone),
    )
}
