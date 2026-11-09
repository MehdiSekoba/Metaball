package ir.mehdisekoba.metaball.util

import kotlin.math.floor
import kotlin.math.sin

/** Port of `shader-hash.util.ts` - deterministic per-drip randomness baked into the shader source. */
internal fun shaderHash(x: Float, y: Float): Float {
    val dot = x * 12.9898f + y * 78f
    val value = (sin(dot.toDouble()).toFloat()) * 43758.5453f
    return value - floor(value)
}
