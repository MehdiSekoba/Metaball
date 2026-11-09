package ir.mehdisekoba.metaball.avatar

import android.graphics.Bitmap

interface MetaballVideoSource {
    val frame: Bitmap?
    val ready: Float
    fun setPlaying(playing: Boolean)
}
