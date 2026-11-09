package ir.mehdisekoba.sample.ui.screen

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.mehdisekoba.sample.ui.Skeleton
import ir.mehdisekoba.sample.ui.SkeletonTone

internal val SkeletonGutter = 20.dp
private val ShelfGap = 12.dp

private val SubtitleWidths = listOf(0.34f, 0.26f, 0.40f, 0.30f, 0.22f)
private val TitleWidths = listOf(0.58f, 0.44f, 0.66f, 0.50f, 0.38f, 0.62f)
private val TileSubtitleWidths = listOf(0.36f, 0.28f, 0.44f)
private val TileTitleWidths = listOf(0.78f, 0.62f, 0.70f, 0.54f)

internal data class SkeletonSong(val id: Int, val subtitleWidth: Float, val titleWidth: Float)
internal data class SkeletonTile(val id: Int, val subtitleWidth: Float, val titleWidth: Float)
internal data class SkeletonShelfSpec(
    val id: String,
    val tileHeight: Dp,
    val tiles: List<SkeletonTile>,
    val tileWidth: Dp,
    val titleWidth: Dp,
)

private val Songs = List(5) {
    SkeletonSong(it, SubtitleWidths[it % SubtitleWidths.size], TitleWidths[it % TitleWidths.size])
}
private val Tiles = List(6) {
    SkeletonTile(it, TileSubtitleWidths[it % TileSubtitleWidths.size], TileTitleWidths[it % TileTitleWidths.size])
}
internal val SkeletonShelves = listOf(
    SkeletonShelfSpec("albums", 220.dp, Tiles, 220.dp, 84.dp),
    SkeletonShelfSpec("videos", 169.dp, Tiles.take(4), 300.dp, 124.dp),
)

@Composable
internal fun SkeletonHeader(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Skeleton(height = 20.dp, modifier = Modifier.width(176.dp))
        Skeleton(height = 12.dp, modifier = Modifier.width(92.dp), tone = SkeletonTone.Soft)
    }
}

@Composable
private fun SkeletonSectionTitle(width: Dp) {
    Skeleton(height = 16.dp, modifier = Modifier.width(width))
}

@Composable
private fun SkeletonSongRow(song: SkeletonSong) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Skeleton(height = 52.dp, radius = 10.dp, modifier = Modifier.size(52.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Skeleton(height = 12.dp, modifier = Modifier.fillMaxWidth(song.titleWidth))
            Skeleton(height = 10.dp, tone = SkeletonTone.Soft, modifier = Modifier.fillMaxWidth(song.subtitleWidth))
        }
    }
}

@Composable
internal fun SkeletonTopSongs(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SkeletonSectionTitle(112.dp)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Songs.forEach { SkeletonSongRow(it) }
        }
    }
}

@Composable
internal fun SkeletonShelf(shelf: SkeletonShelfSpec) {
    val state = rememberLazyListState()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.padding(horizontal = SkeletonGutter)) { SkeletonSectionTitle(shelf.titleWidth) }
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(horizontal = SkeletonGutter),
            horizontalArrangement = Arrangement.spacedBy(ShelfGap),
        ) {
            items(shelf.tiles, key = { it.id }) { tile ->
                Column(Modifier.width(shelf.tileWidth), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Skeleton(height = shelf.tileHeight, radius = 14.dp, modifier = Modifier.fillMaxWidth())
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Skeleton(height = 12.dp, modifier = Modifier.fillMaxWidth(tile.titleWidth))
                        Skeleton(height = 10.dp, tone = SkeletonTone.Soft, modifier = Modifier.fillMaxWidth(tile.subtitleWidth))
                    }
                }
            }
        }
    }
}
