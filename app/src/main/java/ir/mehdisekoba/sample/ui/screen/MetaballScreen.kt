package ir.mehdisekoba.sample.ui.screen

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import ir.mehdisekoba.metaball.avatar.MetaballAvatar
import ir.mehdisekoba.metaball.avatar.MetaballAvatarConfig
import ir.mehdisekoba.metaball.layout.MetaballLayoutConfig
import ir.mehdisekoba.metaball.layout.rememberDisplayCutoutIsland
import ir.mehdisekoba.metaball.layout.rememberMetaballLayout
import ir.mehdisekoba.metaball.scroll.MetaballScrollConfig
import ir.mehdisekoba.metaball.scroll.metaballScroll
import ir.mehdisekoba.metaball.scroll.rememberMetaballScrollState
import ir.mehdisekoba.sample.ui.SurfaceHost
import ir.mehdisekoba.sample.ui.data.ArtistMedia
import ir.mehdisekoba.sample.ui.data.rememberArtistPhoto
import ir.mehdisekoba.sample.ui.data.rememberPalette
import ir.mehdisekoba.sample.ui.rememberMedia3BannerVideo

/**
 * Demo profile screen replicating both modes shown in the reference video:
 * 1) Circle Avatar (00:00 - 00:10): 120x120dp circle melting into the Dynamic Island & expanding into the dripping banner.
 * 2) Custom WxH Squircle Avatar (00:11 - 00:15): 168x136dp rounded-rect avatar melting into the Dynamic Island & expanding into the dripping banner.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetaballScreen() {
    val palette = rememberPalette()
    val density = LocalDensity.current.density
    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Toggle between Circle Mode (00:00-00:10 in video) and Custom WxH Rounded-Rect Mode (00:11-00:15 in video)
    var useCustomRectAvatar by remember { mutableStateOf(false) }

    val layoutConfig = remember(useCustomRectAvatar) {
        if (useCustomRectAvatar) {
            MetaballLayoutConfig(
                avatarWidth = 168.dp,
                avatarHeight = 136.dp,
                avatarCornerRadius = 28.dp,
                bannerHeightRatio = 0.92f,
                alwaysShowIslandPill = true,
            )
        } else {
            MetaballLayoutConfig(
                avatarSize = 120.dp,
                bannerHeightRatio = 0.9f,
                alwaysShowIslandPill = true,
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(palette.background)) {
        val cutoutIsland = rememberDisplayCutoutIsland()
        val layout = rememberMetaballLayout(
            screenWidth = maxWidth,
            screenHeight = maxHeight,
            topInset = topInset,
            island = cutoutIsland,
            config = layoutConfig,
        )
        val avatarConfig = remember { MetaballAvatarConfig() }
        val scrollConfig = remember { MetaballScrollConfig() }
        val scroll = rememberMetaballScrollState(layout, config = scrollConfig)
        val photo = rememberArtistPhoto(ArtistMedia.PHOTO_URL)
        val video = rememberMedia3BannerVideo(ArtistMedia.VIDEO_URL_LOW)

        StatusBarVisibility(hidden = scroll.isLocked)

        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .metaballScroll(scroll)
                    .heightIn(min = layout.minContentHeight.dp)
                    .padding(top = layout.contentInset.dp, bottom = bottomInset + 32.dp),
            ) {
                Column(
                    Modifier.graphicsLayer { translationY = layout.bannerExtra * scroll.expand * density },
                    verticalArrangement = Arrangement.spacedBy(36.dp),
                ) {
                    SkeletonHeader(Modifier.padding(horizontal = SkeletonGutter))
                    SkeletonTopSongs(Modifier.padding(horizontal = SkeletonGutter))
                    SkeletonShelves.forEach { SkeletonShelf(it) }
                }
            }
        }

        MetaballHeader(scroll, layout.avatarCollapse, topInset, palette)
        MetaballAvatar(
            scroll = scroll,
            layout = layout,
            photo = photo,
           video = video,
            placeholderColor = palette.placeholder,
            config = avatarConfig,
        )
        video?.SurfaceHost()


    }
}
