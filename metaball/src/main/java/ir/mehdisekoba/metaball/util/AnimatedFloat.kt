package ir.mehdisekoba.metaball.util

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A snapshot-state float that can be set instantly or animated (like a Reanimated SharedValue). */
internal class AnimatedFloat(private val scope: CoroutineScope, initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set

    private var job: Job? = null

    fun snapTo(target: Float) {
        job?.cancel()
        value = target
    }

    fun animateTo(target: Float, spec: AnimationSpec<Float>) {
        job?.cancel()
        job = scope.launch {
            animate(initialValue = value, targetValue = target, animationSpec = spec) { v, _ -> value = v }
        }
    }
}
