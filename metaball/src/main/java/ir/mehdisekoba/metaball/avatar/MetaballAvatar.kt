package ir.mehdisekoba.metaball.avatar

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ir.mehdisekoba.metaball.layout.MetaballLayout
import ir.mehdisekoba.metaball.scroll.MetaballScrollState
import ir.mehdisekoba.metaball.util.interpolate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

@Composable
fun MetaballAvatar(
    scroll: MetaballScrollState,
    layout: MetaballLayout,
    photo: ImageBitmap?,
    modifier: Modifier = Modifier,
    video: MetaballVideoSource? = null,
    placeholderColor: Color = Color(0xFFC7C7CC),
    config: MetaballAvatarConfig = MetaballAvatarConfig(),
) {
    val motion = remember(layout, config) { MetaballMotion(layout, config) }
    val clock = rememberLiquifyClock(config.liquifySpeed) {
        scroll.expand.coerceIn(0f, 1f) > config.liquifyExpandRange.start
    }

    if (video != null) {
        LaunchedEffect(video, scroll) {
            // Only play video when the banner is locked open, so the initial pull-down drips the photo's color first
            snapshotFlow { scroll.isLocked }
                .distinctUntilChanged()
                .collect { video.setPlaying(it) }
        }
    }

    Box(modifier.fillMaxWidth().height(layout.canvasHeight.dp)) {
        if (Build.VERSION.SDK_INT >= 33) {
            ShaderLayer(scroll, layout, motion, clock, photo?.asAndroidBitmap(), video, placeholderColor, config)
        } else {
            FallbackLayer(scroll, layout, motion, photo, placeholderColor, config)
        }
    }
}

@RequiresApi(33)
@Composable
private fun ShaderLayer(
    scroll: MetaballScrollState,
    layout: MetaballLayout,
    motion: MetaballMotion,
    clock: State<Float>,
    photo: Bitmap?,
    video: MetaballVideoSource?,
    placeholderColor: Color,
    config: MetaballAvatarConfig,
) {
    val density = LocalDensity.current.density
    val renderer = remember(placeholderColor, config) { MetaballShaderRenderer(placeholderColor, config) }
    val videoRange = remember(config) { floatArrayOf(config.videoExpandRange.start, config.videoExpandRange.endInclusive) }
    val zeroOne = remember { floatArrayOf(0f, 1f) }

    Canvas(Modifier.fillMaxSize()) {
        val frame = motion.frame(scroll)
        val videoMix = if (video == null) 0f else {
            interpolate(frame.expand, videoRange, zeroOne) * video.ready
        }
        renderer.update(layout, frame, clock.value, density, photo, video?.frame, videoMix)
        drawRect(brush = renderer.brush)
    }
}

@Composable
private fun FallbackLayer(
    scroll: MetaballScrollState,
    layout: MetaballLayout,
    motion: MetaballMotion,
    photo: ImageBitmap?,
    placeholderColor: Color,
    config: MetaballAvatarConfig,
) {
    val d = LocalDensity.current.density
    Canvas(Modifier.fillMaxSize()) {
        val f = motion.frame(scroll)
        val island = layout.island

        if (f.presence > 0f) {
            val hw = island.halfWidth * f.presence * d
            val hh = island.halfHeight * f.presence * d
            drawRoundRect(
                color = config.tintColor,
                topLeft = Offset(island.centerX * d - hw, island.centerY * d - hh),
                size = Size(hw * 2f, hh * 2f),
                cornerRadius = CornerRadius(hh),
            )
        }

        if (f.radius > 0.5f) {
            val top = lerp(f.centerY - f.radius, 0f, f.expand)
            val bottom = f.centerY + f.radius
            val halfWidth = lerp(f.radius, layout.screenWidth / 2f, f.expand)
            val left = island.centerX - halfWidth
            val right = island.centerX + halfWidth
            val rect = RoundRect(
                left * d, top * d, right * d, bottom * d,
                CornerRadius(f.radius * (1f - f.expand) * d),
            )
            clipPath(Path().apply { addRoundRect(rect) }) {
                if (photo == null) {
                    drawRect(placeholderColor)
                } else {
                    val aspect = (right - left) / (bottom - top)
                    val iw = photo.width.toFloat()
                    val ih = photo.height.toFloat()
                    val cropW = if (iw / ih > aspect) ih * aspect else iw
                    val cropH = if (iw / ih > aspect) ih else iw / aspect
                    drawImage(
                        image = photo,
                        srcOffset = IntOffset(((iw - cropW) / 2f).roundToInt(), ((ih - cropH) * config.cropBiasY).roundToInt()),
                        srcSize = IntSize(cropW.roundToInt(), cropH.roundToInt()),
                        dstOffset = IntOffset((left * d).roundToInt(), (top * d).roundToInt()),
                        dstSize = IntSize(((right - left) * d).roundToInt(), ((bottom - top) * d).roundToInt()),
                    )
                }
                drawRect(config.tintColor, alpha = f.darkness)
            }
        }
    }
}