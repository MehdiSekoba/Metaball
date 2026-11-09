package ir.mehdisekoba.metaball.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** Frame of the "island" the avatar melts into. All values are in dp. */
@Immutable
data class MetaballIslandFrame(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    /** True when the island is always visible (hardware cutout or visible Dynamic Island pill). */
    val isHardware: Boolean,
)

/**
 * Tunable layout constants supporting both circular avatars (00:00-00:07 in reference video)
 * and custom width/height rounded-rectangle avatars & banners (00:11-00:15 in reference video).
 */
@Immutable
data class MetaballLayoutConfig(
    /** Default size when width/height are equal. */
    val avatarSize: Dp = 120.dp,
    /** Custom avatar width (overrides [avatarSize] when specified). */
    val avatarWidth: Dp = avatarSize,
    /** Custom avatar height (overrides [avatarSize] when specified). */
    val avatarHeight: Dp = avatarSize,
    /** Corner radius of the resting avatar. Defaults to half of min(width, height) for a circle, or set e.g. 28.dp for a squircle/rounded rect. */
    val avatarCornerRadius: Dp = min(avatarWidth.value, avatarHeight.value).dp / 2f,
    val avatarTopGap: Dp = 16.dp,
    /** Expanded banner height as a ratio of screen width when [bannerHeight] is null. */
    val bannerHeightRatio: Float = 0.9f,
    /** Optional explicit expanded video/photo banner height in dp. */
    val bannerHeight: Dp? = null,
    /** Optional explicit expanded video/photo banner width in dp (defaults to full screen width). */
    val bannerWidth: Dp? = null,
    val contentGap: Dp = 24.dp,
    /** Dynamic Island pill dimensions. */
    val islandWidth: Dp = 122.dp,
    val islandHeight: Dp = 36.dp,
    val islandTopMargin: Dp = 11.dp,
    /** When true, keeps the Dynamic Island pill visible at the top just like the reference video. */
    val alwaysShowIslandPill: Boolean = true,
    val islandFallbackMinCenter: Dp = 12.dp,
)

@Immutable
data class MetaballLayout(
    val avatarCenterY: Float,
    val avatarCollapse: Float,
    val avatarRadius: Float,
    val avatarHalfWidth: Float,
    val avatarHalfHeight: Float,
    val avatarCornerRadius: Float,
    val bannerWidth: Float,
    val bannerHeight: Float,
    val bannerExtra: Float,
    val canvasHeight: Float,
    val contentInset: Float,
    val island: MetaballIslandFrame,
    val minContentHeight: Float,
    val pixel: Float,
    val screenWidth: Float,
    val snapOffsets: List<Float>,
) {
    companion object {
        internal fun create(
            width: Float,
            height: Float,
            top: Float,
            islandOverride: MetaballIslandFrame?,
            config: MetaballLayoutConfig,
            density: Float,
        ): MetaballLayout {
            val island = islandOverride ?: resolveIslandFrame(top, width, config)
            val avatarHalfW = config.avatarWidth.value / 2f
            val avatarHalfH = config.avatarHeight.value / 2f
            val avatarRadius = min(avatarHalfW, avatarHalfH)
            val cornerRadius = config.avatarCornerRadius.value.coerceIn(0f, avatarRadius)
            val effectiveTop = max(top, island.centerY + island.halfHeight)
            val avatarTop = effectiveTop + config.avatarTopGap.value
            val avatarCenterY = avatarTop + avatarHalfH
            val avatarCollapse = avatarCenterY - island.centerY
            val contentInset = avatarTop + config.avatarHeight.value + config.contentGap.value
            val targetBannerW = config.bannerWidth?.value ?: width
            val targetBannerH = config.bannerHeight?.value ?: (width * config.bannerHeightRatio)
            val bannerExtra = max(0f, targetBannerH + config.contentGap.value - contentInset)
            return MetaballLayout(
                avatarCenterY = avatarCenterY,
                avatarCollapse = avatarCollapse,
                avatarRadius = avatarRadius,
                avatarHalfWidth = avatarHalfW,
                avatarHalfHeight = avatarHalfH,
                avatarCornerRadius = cornerRadius,
                bannerWidth = targetBannerW,
                bannerHeight = targetBannerH,
                bannerExtra = bannerExtra,
                canvasHeight = height,
                contentInset = contentInset,
                island = island,
                minContentHeight = height + avatarCollapse,
                pixel = 1f / density,
                screenWidth = width,
                snapOffsets = listOf(0f, avatarCollapse),
            )
        }
    }
}

@Composable
fun rememberMetaballLayout(
    screenWidth: Dp,
    screenHeight: Dp,
    topInset: Dp,
    island: MetaballIslandFrame? = null,
    config: MetaballLayoutConfig = MetaballLayoutConfig(),
): MetaballLayout {
    val density = LocalDensity.current.density
    return remember(screenWidth, screenHeight, topInset, island, config, density) {
        MetaballLayout.create(screenWidth.value, screenHeight.value, topInset.value, island, config, density)
    }
}

@Composable
fun rememberDisplayCutoutIsland(): MetaballIslandFrame? {
    val view = LocalView.current
    val density = LocalDensity.current.density
    var frame by remember { mutableStateOf<MetaballIslandFrame?>(null) }
    LaunchedEffect(view, density) {
        withFrameNanos { }
        val rect = view.rootWindowInsets?.displayCutout?.boundingRectTop
        frame = if (rect != null && !rect.isEmpty) {
            MetaballIslandFrame(
                centerX = rect.exactCenterX() / density,
                centerY = rect.exactCenterY() / density,
                halfWidth = rect.width() / 2f / density,
                halfHeight = rect.height() / 2f / density,
                isHardware = true,
            )
        } else {
            null
        }
    }
    return frame
}
