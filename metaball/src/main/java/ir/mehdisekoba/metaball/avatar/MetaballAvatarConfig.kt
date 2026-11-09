package ir.mehdisekoba.metaball.avatar



import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MetaballDripConfig(
    val count: Int = 12,
    val neckRange: ClosedFloatingPointRange<Float> = 3.5f..6.5f,
    val bulbExtraRange: ClosedFloatingPointRange<Float> = 2.5f..5.0f,
    val lengthRange: ClosedFloatingPointRange<Float> = 0.35f..1.0f,
    val cycleSecondsRange: ClosedFloatingPointRange<Float> = 2.6f..4.4f,
    val activeFraction: Float = 1f,
) {
    init {
        require(count in 0..32)
        require(activeFraction in 0f..1f)
    }
}

@Immutable
data class MetaballAvatarConfig(
    val tintColor: Color = Color.Black,
    val blendRange: ClosedFloatingPointRange<Float> = 0.05f..0.8f,
    val blurRange: ClosedFloatingPointRange<Float> = 0.08f..0.5f,
    val darkenRange: ClosedFloatingPointRange<Float> = 0.6f..0.95f,
    val dropReachRange: ClosedFloatingPointRange<Float> = 0.22f..0.4f,
    val blurMax: Float = 30f,
    val cropBiasY: Float = 0.3f,
    val dropTipRatio: Float = 0.22f,
    val radiusMidRatio: Float = 0.62f,
    val radiusEndRatio: Float = 0f,
    val pullDistance: Float = 160f,
    val pullScale: Float = 1f,
    val gooMin: Float = 20f,
    val gooMax: Float = 70f,
    val gooRamp: Float = 0.3f,
    val gooFade: Float = 0.78f,
    val presenceStops: List<Float> = listOf(0f, 0.4f, 0.9f, 1.1f),
    val presenceScales: List<Float> = listOf(0f, 1f, 1f, 0f),
    val fadeSoftness: Float = 0.8f,
    val fadeSoftnessMin: Float = 8f,
    // Start liquid drips immediately as the banner opens
    val liquifyExpandRange: ClosedFloatingPointRange<Float> = 0.15f..0.85f,
    val liquifyAmplitude: Float = 6f,
    val liquifyScale: Float = 70f,
    val liquifyReach: Float = 85f,
    val liquifySpeed: Float = 0.85f,
    val liquifyMeltStart: Float = 0.86f,
    val liquifyMeltPull: Float = 0.06f,
    val drips: MetaballDripConfig = MetaballDripConfig(),
    val videoCropBiasY: Float = 0.5f,
    val videoExpandRange: ClosedFloatingPointRange<Float> = 0.35f..0.85f,
) {
    init {
        require(presenceStops.size == presenceScales.size && presenceStops.size >= 2) {
            "presenceStops and presenceScales must have the same size (>= 2)"
        }
    }
}
