package ir.mehdisekoba.sample.ui

import ir.mehdisekoba.metaball.avatar.MetaballVideoSource
import android.graphics.Bitmap
import android.view.Choreographer
import android.view.TextureView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class Media3BannerVideo internal constructor(
    private val player: ExoPlayer,
    internal val textureView: TextureView,
    private val scope: CoroutineScope,
    private val fadeMillis: Int,
) : MetaballVideoSource {

    override var frame: Bitmap? by mutableStateOf(null, neverEqualPolicy())
        private set

    private val readyAnim = Animatable(0f)
    override val ready: Float get() = readyAnim.value

    private var playing = false
    private var hasRenderedFirstFrame = false
    private var fadeTriggered = false

    private val listener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            hasRenderedFirstFrame = true
        }
    }

    init {
        player.addListener(listener)
    }

    private val pump = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!playing) return
            if (hasRenderedFirstFrame) {
                capture()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun capture() {
        val size = player.videoSize
        if (size.width <= 0 || size.height <= 0 || !textureView.isAvailable) return
        val bitmap = textureView.getBitmap(size.width, size.height) ?: return
        frame = bitmap
        if (!fadeTriggered) {
            fadeTriggered = true
            scope.launch {
                // Let the initial photo color drip down for 600ms first, then fade in the video
                delay(600.milliseconds)
                readyAnim.animateTo(1f, tween(fadeMillis))
            }
        }
    }

    override fun setPlaying(playing: Boolean) {
        if (this.playing == playing) return
        this.playing = playing
        player.playWhenReady = playing
        if (playing) {
            fadeTriggered = false
            scope.launch { readyAnim.snapTo(0f) }
            Choreographer.getInstance().postFrameCallback(pump)
        } else {
            Choreographer.getInstance().removeFrameCallback(pump)
            scope.launch { readyAnim.animateTo(0f, tween(180)) }
        }
    }

    internal fun release() {
        playing = false
        player.removeListener(listener)
        Choreographer.getInstance().removeFrameCallback(pump)
        player.release()
    }
}

@Composable
fun rememberMedia3BannerVideo(
    url: String?,
    fadeMillis: Int = 500,
    volume: Float = 0f,
    looping: Boolean = true,
): Media3BannerVideo? {
    if (url == null) return null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val video = remember(url, fadeMillis, volume, looping) {
        val textureView = TextureView(context)
        val player = ExoPlayer.Builder(context).build().apply {
            repeatMode = if (looping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            this.volume = volume
            setVideoTextureView(textureView)
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = false
            prepare()
        }
        Media3BannerVideo(player, textureView, scope, fadeMillis)
    }
    DisposableEffect(video) { onDispose { video.release() } }
    return video
}

@Composable
fun Media3BannerVideo.SurfaceHost(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { textureView },
        modifier = modifier.size(1.dp).alpha(0.01f),
    )
}