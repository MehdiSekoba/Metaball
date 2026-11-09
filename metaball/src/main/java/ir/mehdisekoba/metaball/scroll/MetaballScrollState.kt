package ir.mehdisekoba.metaball.scroll

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import ir.mehdisekoba.metaball.layout.MetaballLayout
import ir.mehdisekoba.metaball.util.AnimatedFloat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
class MetaballScrollState internal constructor(
    val scrollState: ScrollState,
    private val density: Float,
    private val collapse: Float,
    private val scope: CoroutineScope,
    private val config: MetaballScrollConfig,
) {
    private val pull = AnimatedFloat(scope, 0f)
    private val expandValue = AnimatedFloat(scope, 0f)
    private val calmValue = AnimatedFloat(scope, 0f)
    private val anchorValue = AnimatedFloat(scope, 0f)

    var isLocked by mutableStateOf(false)
        private set

    private var isHolding by mutableStateOf(false)
    private var isDragging = false

    val expand: Float get() = expandValue.value
    val calm: Float get() = calmValue.value
    val anchor: Float get() = anchorValue.value
    val scrollY: Float get() = scrollState.value / density - pull.value

    internal val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            beginDrag()
            if (config.pullToOpenEnabled && pull.value > 0f && available.y < 0f) {
                val dragDp = -available.y / density
                val used = min(pull.value / config.pullResistance, dragDp)
                pull.snapTo(pull.value - used * config.pullResistance)
                return Offset(0f, -used * density)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (config.pullToOpenEnabled && source == NestedScrollSource.UserInput && available.y > 0f) {
                pull.snapTo(pull.value + available.y / density * config.pullResistance)
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            endDrag()
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle()
            return Velocity.Zero
        }
    }

    internal suspend fun observe() = coroutineScope {
        launch { snapshotFlow { scrollY }.collect { onScroll(it) } }
        launch {
            snapshotFlow { isHolding && expand < config.settleExpand && calm == 0f }.collect { settled ->
                if (settled) {
                    isHolding = false
                    anchorValue.animateTo(0f, config.bannerSpring)
                }
            }
        }
    }

    private fun beginDrag() {
        if (isDragging) return
        isDragging = true
        if (calm > 0f) calmValue.animateTo(0f, tween(config.calmFadeMillis))
    }

    private fun endDrag() {
        isDragging = false
        if (pull.value > 0f) {
            pull.animateTo(0f, config.pullBackSpring)
        }
        val offsetDp = scrollState.value / density
        if (isHolding && offsetDp > config.restEpsilon) {
            calmValue.snapTo(1f)
            scope.launch {
                withFrameNanos { }
                scrollState.animateScrollTo(0, config.scrollSpec)
            }
        }
    }

    private fun settle() {
        val offsetDp = scrollState.value / density
        if (calm > 0f && offsetDp > config.restEpsilon) {
            scope.launch { scrollState.animateScrollTo(0, config.scrollSpec) }
            return
        }
        if (config.snapToCollapse && !isLocked && calm == 0f &&
            offsetDp > config.restEpsilon && offsetDp < collapse - config.restEpsilon
        ) {
            val target = if (offsetDp > collapse / 2f) collapse else 0f
            scope.launch { scrollState.animateScrollTo((target * density).roundToInt(), config.scrollSpec) }
        }
    }

    private fun onScroll(offset: Float) {
        if (!isLocked && isDragging && offset < -config.lockPull) {
            isLocked = true
            isHolding = false
            anchorValue.snapTo(0f)
            calmValue.snapTo(0f)
            expandValue.animateTo(1f, config.bannerSpring)
        } else if (isLocked && offset > config.unlockOffset) {
            isLocked = false
            isHolding = true
            anchorValue.snapTo(offset)
            expandValue.animateTo(0f, config.bannerSpring)
            if (!isDragging) {
                calmValue.snapTo(1f)
                scope.launch { scrollState.animateScrollTo(0, config.scrollSpec) }
            }
        } else if (!isLocked) {
            if (isHolding) anchorValue.snapTo(max(offset, 0f))
            if (offset <= config.restEpsilon) {
                if (anchorValue.value > 0f) anchorValue.snapTo(0f)
                if (calm > 0f && !isDragging) calmValue.animateTo(0f, tween(config.calmFadeMillis))
            }
        }
    }
}

@Composable
fun rememberMetaballScrollState(
    layout: MetaballLayout,
    scrollState: ScrollState = rememberScrollState(),
    config: MetaballScrollConfig = MetaballScrollConfig(),
): MetaballScrollState {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val state = remember(scrollState, density, layout.avatarCollapse, scope, config) {
        MetaballScrollState(scrollState, density, layout.avatarCollapse, scope, config)
    }
    LaunchedEffect(state) { state.observe() }
    return state
}

fun Modifier.metaballScroll(state: MetaballScrollState): Modifier =
    this
        .nestedScroll(state.connection)
        .verticalScroll(state.scrollState)
