package ir.mehdisekoba.metaball.scroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import kotlin.math.sqrt

@Immutable
data class MetaballScrollConfig(
    val lockPull: Float = 70f,
    val unlockOffset: Float = 1f,
    val restEpsilon: Float = 0.5f,
    val settleExpand: Float = 0.02f,
    val calmFadeMillis: Int = 180,
    val pullResistance: Float = 0.55f,
    val pullToOpenEnabled: Boolean = true,
    val snapToCollapse: Boolean = true,
    val bannerSpring: AnimationSpec<Float> = spring(dampingRatio = 30f / (2f * sqrt(260f)), stiffness = 260f),
    val pullBackSpring: AnimationSpec<Float> = spring(stiffness = Spring.StiffnessMediumLow),
    val scrollSpec: AnimationSpec<Float> = spring(visibilityThreshold = 1f),
)
