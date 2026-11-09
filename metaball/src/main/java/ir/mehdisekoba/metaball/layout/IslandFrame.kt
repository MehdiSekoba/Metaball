package ir.mehdisekoba.metaball.layout

import kotlin.math.max

/**
 * Resolves the target Dynamic Island frame.
 * When [config.alwaysShowIslandPill] is true (default), renders the full iOS-style Dynamic Island
 * pill (124x36dp by default) at the top center, matching the reference video.
 */
internal fun resolveIslandFrame(
    insetTop: Float,
    screenWidth: Float,
    config: MetaballLayoutConfig,
): MetaballIslandFrame {
    val width = config.islandWidth.value
    val height = config.islandHeight.value
    val centerY = if (config.alwaysShowIslandPill) {
        config.islandTopMargin.value + height / 2f
    } else {
        max(insetTop / 2f, config.islandFallbackMinCenter.value)
    }
    return MetaballIslandFrame(
        centerX = screenWidth / 2f,
        centerY = centerY,
        halfWidth = width / 2f,
        halfHeight = height / 2f,
        isHardware = config.alwaysShowIslandPill,
    )
}
