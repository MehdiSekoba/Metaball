package ir.mehdisekoba.metaball.avatar

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import ir.mehdisekoba.metaball.layout.MetaballLayout

@RequiresApi(33)
internal class MetaballShaderRenderer(placeholderColor: Color, private val c: MetaballAvatarConfig) {
    private val shader = RuntimeShader(MetaballShader.source(c.drips))
    val brush: Brush = ShaderBrush(shader)

    private val placeholder = BitmapShader(
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).also { it.eraseColor(placeholderColor.toArgb()) },
        Shader.TileMode.CLAMP,
        Shader.TileMode.CLAMP,
    )

    private var photoSource: Bitmap? = null
    private var photoShader: Shader? = null

    private fun linearShader(bitmap: Bitmap): BitmapShader =
        BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
            it.filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

    fun update(
        layout: MetaballLayout,
        frame: MetaballMotionFrame,
        flow: Float,
        density: Float,
        photo: Bitmap?,
        videoFrame: Bitmap?,
        videoMix: Float,
    ) {
        if (photo !== photoSource) {
            photoSource = photo
            photoShader = photo?.let { linearShader(it) }
        }
        val island = layout.island
        val photoW = photo?.width?.toFloat() ?: 1f
        val photoH = photo?.height?.toFloat() ?: 1f
        val useVideo = videoFrame != null && videoMix > 0f
        val videoW = if (useVideo) videoFrame.width.toFloat() else 1f
        val videoH = if (useVideo) videoFrame.height.toFloat() else 1f

        shader.setFloatUniform("uDensity", density)
        shader.setFloatUniform("uTint", c.tintColor.red, c.tintColor.green, c.tintColor.blue)
        shader.setFloatUniform("uAvatar", island.centerX, frame.centerY, frame.radius, frame.darkness)
        shader.setFloatUniform("uAvatarExtent", frame.halfWidth, frame.halfHeight, frame.cornerRadius)
        shader.setFloatUniform("uBanner", frame.expand, layout.bannerWidth)
        shader.setFloatUniform("uDrop", frame.stretch, frame.tip)
        shader.setFloatUniform("uEffect", frame.blur, frame.goo, frame.presence, layout.pixel)
        shader.setFloatUniform("uFade", c.fadeSoftness, c.fadeSoftnessMin)
        shader.setFloatUniform("uFlow", flow, frame.liquify)
        shader.setFloatUniform("uImage", photoW, photoH, c.cropBiasY, frame.blend)
        shader.setFloatUniform(
            "uLiquify", c.liquifyAmplitude, c.liquifyScale, c.liquifyReach, layout.canvasHeight,
        )
        shader.setFloatUniform("uMelt", c.liquifyMeltStart, c.liquifyMeltPull)
        shader.setFloatUniform("uIsland", island.centerX, island.centerY, island.halfWidth, island.halfHeight)
        shader.setFloatUniform("uVideoImage", videoW, videoH, c.videoCropBiasY, if (useVideo) videoMix else 0f)

        shader.setInputShader("uPhoto", photoShader ?: placeholder)
        shader.setInputShader("uVideo", if (useVideo) linearShader(videoFrame) else placeholder)
    }
}
