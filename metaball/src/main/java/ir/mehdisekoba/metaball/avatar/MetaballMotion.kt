package ir.mehdisekoba.metaball.avatar



import ir.mehdisekoba.metaball.layout.MetaballLayout
import ir.mehdisekoba.metaball.scroll.MetaballScrollState
import ir.mehdisekoba.metaball.util.Extrapolation
import ir.mehdisekoba.metaball.util.interpolate
import kotlin.math.max

internal class MetaballMotionFrame(
    val blend: Float,
    val blur: Float,
    val centerY: Float,
    val darkness: Float,
    val expand: Float,
    val goo: Float,
    val liquify: Float,
    val presence: Float,
    val radius: Float,
    val halfWidth: Float,
    val halfHeight: Float,
    val cornerRadius: Float,
    val stretch: Float,
    val tip: Float,
)

internal class MetaballMotion(private val layout: MetaballLayout, private val c: MetaballAvatarConfig) {
    private val collapse = layout.avatarCollapse
    private val restCenterY = layout.avatarCenterY
    private val restRadius = layout.avatarRadius
    private val restHalfW = layout.avatarHalfWidth
    private val restHalfH = layout.avatarHalfHeight
    private val restCorner = layout.avatarCornerRadius
    private val islandCenterY = layout.island.centerY
    private val islandBottom = islandCenterY + layout.island.halfHeight
    private val isHardware = layout.island.isHardware
    private val endRadius = layout.island.halfHeight * c.radiusEndRatio

    private val zeroOne = floatArrayOf(0f, 1f)
    private val blend = scaled(c.blendRange)
    private val blur = scaled(c.blurRange)
    private val darken = scaled(c.darkenRange)
    private val reach = scaled(c.dropReachRange)
    private val gooEnd = collapse * c.gooRamp
    private val gooFade = collapse * c.gooFade
    private val presenceStops = FloatArray(c.presenceStops.size) { c.presenceStops[it] * collapse }
    private val presenceScales = c.presenceScales.toFloatArray()
    private val liquifyRange = floatArrayOf(c.liquifyExpandRange.start, c.liquifyExpandRange.endInclusive)

    private val centerYIn = floatArrayOf(0f, collapse)
    private val centerYOut = floatArrayOf(restCenterY, islandCenterY)
    private val radiusIn = floatArrayOf(-c.pullDistance, 0f, collapse / 2f, collapse)
    private val radiusOut = floatArrayOf(
        restRadius * c.pullScale, restRadius, restRadius * c.radiusMidRatio, endRadius,
    )
    private val blurOut = floatArrayOf(0f, c.blurMax)
    private val gooIn = floatArrayOf(0f, gooEnd, gooFade, collapse)
    private val gooOut = floatArrayOf(c.gooMin, c.gooMax, c.gooMax, 0f)

    private fun scaled(range: ClosedFloatingPointRange<Float>) =
        floatArrayOf(range.start * collapse, range.endInclusive * collapse)

    fun frame(scroll: MetaballScrollState): MetaballMotionFrame {
        val progress = scroll.expand
        val expand = progress.coerceIn(0f, 1f)
        val gate = (1f - expand) * (1f - scroll.calm)

        val offset = scroll.scrollY
        val travel = if (offset <= 0f) offset else max(offset - scroll.anchor, 0f)

        val centerY = interpolate(travel, centerYIn, centerYOut, Extrapolation.EXTEND, Extrapolation.CLAMP) +
                layout.bannerExtra * progress
        val radius = interpolate(travel, radiusIn, radiusOut)
        val scaleRatio = if (restRadius > 0.001f) radius / restRadius else 0f
        val morphToCircle = (travel / (collapse * 0.45f)).coerceIn(0f, 1f)
        val halfW = (restHalfW * scaleRatio) * (1f - morphToCircle) + radius * morphToCircle
        val halfH = (restHalfH * scaleRatio) * (1f - morphToCircle) + radius * morphToCircle
        val corner = (restCorner * scaleRatio) * (1f - morphToCircle) + radius * morphToCircle

        val tip = radius * c.dropTipRatio
        val base = radius - tip
        val full = max(base, centerY - islandBottom)
        val reachAmount = interpolate(travel, reach, zeroOne)

        return MetaballMotionFrame(
            blend = interpolate(travel, blend, zeroOne) * (1f - expand),
            blur = interpolate(travel, blur, blurOut) * (1f - expand),
            centerY = centerY,
            darkness = interpolate(travel, darken, zeroOne) * (1f - expand),
            expand = expand,
            goo = interpolate(travel, gooIn, gooOut) * gate,
            liquify = interpolate(expand, liquifyRange, zeroOne),
            presence = (if (isHardware) 1f else interpolate(travel, presenceStops, presenceScales)) * gate,
            radius = radius,
            halfWidth = halfW,
            halfHeight = halfH,
            cornerRadius = corner,
            stretch = base + (full - base) * reachAmount,
            tip = tip,
        )
    }
}