package ir.mehdisekoba.metaball.util

internal enum class Extrapolation { CLAMP, EXTEND }

/**
 * Piecewise-linear interpolation, equivalent to Reanimated's `interpolate`.
 * [input] must be monotonically increasing and have the same size as [output].
 */
internal fun interpolate(
    x: Float,
    input: FloatArray,
    output: FloatArray,
    left: Extrapolation = Extrapolation.CLAMP,
    right: Extrapolation = Extrapolation.CLAMP,
): Float {
    val n = input.size
    if (x <= input[0]) {
        return if (left == Extrapolation.CLAMP) output[0] else segment(x, input[0], input[1], output[0], output[1])
    }
    if (x >= input[n - 1]) {
        return if (right == Extrapolation.CLAMP) output[n - 1]
        else segment(x, input[n - 2], input[n - 1], output[n - 2], output[n - 1])
    }
    var i = 1
    while (i < n - 1 && x > input[i]) i++
    return segment(x, input[i - 1], input[i], output[i - 1], output[i])
}

private fun segment(x: Float, x0: Float, x1: Float, y0: Float, y1: Float): Float {
    val d = x1 - x0
    return if (d == 0f) y1 else y0 + (y1 - y0) * (x - x0) / d
}
