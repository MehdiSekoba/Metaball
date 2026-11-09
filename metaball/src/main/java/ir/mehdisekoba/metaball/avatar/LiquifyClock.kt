package ir.mehdisekoba.metaball.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun rememberLiquifyClock(
    speed: Float,
    isFlowing: () -> Boolean
): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val latestIsFlowing = rememberUpdatedState(isFlowing)

    LaunchedEffect(speed) {
        snapshotFlow { latestIsFlowing.value() }
            .distinctUntilChanged()
            .collectLatest { flowing ->
                if (flowing) {
                    time.floatValue = 0f

                    var last = withFrameNanos { it }

                    while (true) {
                        val now = withFrameNanos { it }

                        time.floatValue +=
                            (now - last) / 1_000_000_000f * speed

                        last = now
                    }
                }
            }
    }

    return time
}