# Metaball Compose

An Android library built with **Jetpack Compose** for creating animated metaball effects. It transforms an avatar into a wide banner, visually connects the liquid shape to the top display cutout (Dynamic Island-style area), animates the banner's lower edge with stretching and dripping details, and supports image or video-frame compositing.

> This documentation is based on the source project. Features implemented by the sample app are identified separately from the public API of the `metaball` library module.

## Contents

- [Features](#features)
- [Requirements and compatibility](#requirements-and-compatibility)
- [Demo video](#demo-video)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Layout configuration](#layout-configuration)
- [Avatar and drip configuration](#avatar-and-drip-configuration)
- [Scroll configuration](#scroll-configuration)
- [Video integration](#video-integration)
- [How it works](#how-it-works)
- [Android version behavior](#android-version-behavior)
- [Sample app](#sample-app)
- [Limitations and notes](#limitations-and-notes)
- [Build](#build)
- [Publishing and versioning](#publishing-and-versioning)
- [License](#license)

## Features

- Circular or rounded-rectangle avatars with configurable dimensions.
- Avatar-to-banner transformation driven by scrolling and expansion.
- Visual connection between the liquid shape and the device's Dynamic Island/display cutout area.
- Automatic display-cutout detection through `rememberDisplayCutoutIsland()`.
- Manually configurable Island position and dimensions.
- Liquid movement and stretching along the banner's lower edge.
- Shader source generation based on drip configuration, with caching for matching configurations.
- Image input through `ImageBitmap`.
- Optional video-frame input through the `MetaballVideoSource` interface.
- Scroll state with configurable pull-to-open, snapping, and spring behavior.
- Canvas-based fallback for devices where the RuntimeShader path is unavailable.

## Requirements and compatibility

| Item | Project configuration |
|---|---|
| Android `minSdk` | 31 |
| `compileSdk` | 37 |
| Language | Kotlin |
| UI toolkit | Jetpack Compose |
| Java compatibility | 17 |
| Runtime shader path | Android 13 / API 33 and above |
| Sample modules | `:app` and `:metaball` |

The RuntimeShader rendering path requires API 33 or higher. On API 31 and 32, the library uses a simpler Canvas-based fallback. The fallback should not be expected to reproduce every shader detail, procedural drip, or video-compositing effect.

## Demo

<video src="https://github.com/user-attachments/assets/ca779d24-2d32-4f05-b86b-0b9b77a84646" controls width="100%">
</video>


If your Git hosting platform supports inline video previews, open the linked MP4 to play it. The video is a demonstration asset; it is not required to build or use the library.


## Installation

The library is intended to be consumed from **Maven Central**. The coordinates below are placeholders: replace `YOUR_GROUP_ID` and `1.0.0` with the actual group and published version you choose. The artifact name shown here is `metaball`.

### 1. Add Maven Central

Make sure Maven Central is included in your dependency repositories. In a typical `settings.gradle.kts` setup:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

If your project manages repositories in the root build file instead, add `mavenCentral()` there. Do not declare the same repository in multiple places if your project uses `FAIL_ON_PROJECT_REPOS`.

### 2. Add the dependency directly

In the consuming module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.mehdisekoba:metaball:1.0.0")
}
```

### 3. Or declare it in a Gradle Version Catalog

Add an alias to `gradle/libs.versions.toml`:

```toml
[versions]
metaball = "1.0.0"

[libraries]
metaball = { module = "io.github.mehdisekoba:metaball", version.ref = "metaball" }
```

Then use the catalog alias in your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.metaball)
}
```

Replace the placeholder group ID and version with the values used for your Maven Central publication. The sample project itself uses a Gradle Version Catalog in `gradle/libs.versions.toml`; the catalog is a dependency-management mechanism and is separate from the published Maven coordinates.

### Local multi-module development

When working on this repository rather than consuming the published artifact, you can include the library module directly:

In `settings.gradle.kts`:

```kotlin
include(":app")
include(":metaball")
```

In the app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":metaball"))
}
```

## Quick start

The basic integration consists of three parts: calculate the layout, create a shared scroll state, and place `MetaballAvatar` in the same screen container.

```kotlin
@Composable
fun ProfileScreen(
    photo: ImageBitmap?,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val topInset = WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

        val island = rememberDisplayCutoutIsland()

        val layout = rememberMetaballLayout(
            screenWidth = maxWidth,
            screenHeight = maxHeight,
            topInset = topInset,
            island = island,
            config = MetaballLayoutConfig(
                avatarSize = 120.dp,
                avatarTopGap = 16.dp,
                bannerHeightRatio = 0.9f,
            ),
        )

        val scroll = rememberMetaballScrollState(
            layout = layout,
            config = MetaballScrollConfig(),
        )

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .metaballScroll(scroll)
                    .heightIn(min = layout.minContentHeight.dp)
                    .padding(top = layout.contentInset.dp),
            ) {
                // Your scrollable screen content
            }

            MetaballAvatar(
                scroll = scroll,
                layout = layout,
                photo = photo,
                placeholderColor = Color.LightGray,
                config = MetaballAvatarConfig(),
            )
        }
    }
}
```

Main imports:

```kotlin
import ir.mehdisekoba.metaball.avatar.MetaballAvatar
import ir.mehdisekoba.metaball.avatar.MetaballAvatarConfig
import ir.mehdisekoba.metaball.layout.MetaballLayoutConfig
import ir.mehdisekoba.metaball.layout.rememberDisplayCutoutIsland
import ir.mehdisekoba.metaball.layout.rememberMetaballLayout
import ir.mehdisekoba.metaball.scroll.MetaballScrollConfig
import ir.mehdisekoba.metaball.scroll.metaballScroll
import ir.mehdisekoba.metaball.scroll.rememberMetaballScrollState
```

Important integration notes:

1. Place `MetaballAvatar` in a container whose dimensions are consistent with `layout.canvasHeight`. The sample places it in the same `BoxWithConstraints` as the screen content.
2. Attach the screen's scrollable content to the shared state with `metaballScroll(scroll)`.
3. `photo` is an `ImageBitmap?`. Image loading belongs to the host application; the library does not download image URLs.
4. For a correctly aligned banner, coordinate the scroll content with `layout.contentInset` and `layout.minContentHeight`.

## Layout configuration

`MetaballLayoutConfig` defines the geometry and spacing of the avatar, banner, and Island area.

| Parameter | Default | Description |
|---|---:|---|
| `avatarSize` | `120.dp` | Default square/circular avatar size |
| `avatarWidth` | `avatarSize` | Custom avatar width |
| `avatarHeight` | `avatarSize` | Custom avatar height |
| `avatarCornerRadius` | Half the smaller dimension | Avatar corner radius |
| `avatarTopGap` | `16.dp` | Gap above the avatar |
| `bannerHeightRatio` | `0.9f` | Banner height relative to screen width when no explicit height is supplied |
| `bannerHeight` | `null` | Explicit banner height |
| `bannerWidth` | `null` | Explicit banner width; defaults to screen width |
| `contentGap` | `24.dp` | Screen content spacing |
| `islandWidth` | `122.dp` | Fallback Island width |
| `islandHeight` | `36.dp` | Fallback Island height |
| `islandTopMargin` | `11.dp` | Fallback Island top margin |
| `alwaysShowIslandPill` | `true` | Controls the pill in the layout geometry |
| `islandFallbackMinCenter` | `12.dp` | Minimum fallback center value |

For a circular avatar, `avatarSize` is usually sufficient. For a rounded rectangle, configure `avatarWidth`, `avatarHeight`, and `avatarCornerRadius`:

```kotlin
val layoutConfig = MetaballLayoutConfig(
    avatarWidth = 168.dp,
    avatarHeight = 136.dp,
    avatarCornerRadius = 28.dp,
    bannerHeightRatio = 0.92f,
)
```

### Display cutout detection

On devices that expose a display cutout, use:

```kotlin
val island = rememberDisplayCutoutIsland()
```

This function reads the cutout bounding rectangle from `rootWindowInsets` and converts its dimensions from pixels to dp. If no cutout is available, it returns `null`, and the layout uses the fallback values from the config.

If you need a custom position, create a `MetaballIslandFrame` and pass it to `rememberMetaballLayout`. Frame values are expressed in dp:

```kotlin
val customIsland = MetaballIslandFrame(
    centerX = 196f,
    centerY = 28f,
    halfWidth = 61f,
    halfHeight = 18f,
    isHardware = false,
)
```

These values are illustrative only. Replace them with measurements appropriate for the target device.

## Avatar and drip configuration

`MetaballAvatarConfig` controls the avatar appearance and the shader/motion ranges.

| Parameter | Default | Description |
|---|---:|---|
| `tintColor` | `Color.Black` | Tint color |
| `blendRange` | `0.05f..0.8f` | Image blending range during motion |
| `blurRange` | `0.08f..0.5f` | Blur range |
| `darkenRange` | `0.6f..0.95f` | Darkening range |
| `dropReachRange` | `0.22f..0.4f` | Range for the stretching shape to reach the Island |
| `blurMax` | `30f` | Maximum blur |
| `cropBiasY` | `0.3f` | Vertical image-crop bias |
| `dropTipRatio` | `0.22f` | Drop-tip ratio |
| `radiusMidRatio` | `0.62f` | Radius ratio at the middle point |
| `radiusEndRatio` | `0f` | Radius ratio at the end of the motion |
| `pullDistance` | `160f` | Pull distance |
| `pullScale` | `1f` | Pull scale factor |
| `gooMin` / `gooMax` | `20f` / `70f` | Minimum and maximum goo values |
| `gooRamp` / `gooFade` | `0.3f` / `0.78f` | Goo ramp and fade points |
| `presenceStops` | `[0, .4, .9, 1.1]` | Island-presence control points |
| `presenceScales` | `[0, 1, 1, 0]` | Corresponding presence values |
| `fadeSoftness` | `0.8f` | Fade softness |
| `fadeSoftnessMin` | `8f` | Minimum fade softness |
| `liquifyExpandRange` | `0.15f..0.85f` | Expansion range where liquify becomes active |
| `liquifyAmplitude` | `6f` | Liquid-wave amplitude |
| `liquifyScale` | `70f` | Liquid-wave scale |
| `liquifyReach` | `85f` | Liquid-wave reach |
| `liquifySpeed` | `0.85f` | Effect clock speed |
| `liquifyMeltStart` | `0.86f` | Melt start point |
| `liquifyMeltPull` | `0.06f` | Melt pull factor |
| `drips` | `MetaballDripConfig()` | Drip configuration |
| `videoCropBiasY` | `0.5f` | Vertical crop bias for video |
| `videoExpandRange` | `0.35f..0.85f` | Expansion range used for video blending |

### Drip configuration

`MetaballDripConfig` defines the procedural drips along the banner edge.

| Parameter | Default | Description |
|---|---:|---|
| `count` | `12` | Number of procedural drips; clamped to 0–32 |
| `neckRange` | `3.5f..6.5f` | Drip-neck thickness range |
| `bulbExtraRange` | `2.5f..5.0f` | Additional bulb-size range |
| `lengthRange` | `0.35f..1.0f` | Drip-length range |
| `cycleSecondsRange` | `2.6f..4.4f` | Drip-cycle duration range |
| `activeFraction` | `1f` | Drip activity multiplier; clamped to 0–1 |

Example customization:

```kotlin
val avatarConfig = MetaballAvatarConfig(
    tintColor = Color(0xFF111111),
    liquifyAmplitude = 8f,
    liquifyScale = 64f,
    liquifySpeed = 0.9f,
    drips = MetaballDripConfig(
        count = 14,
        neckRange = 3f..5.5f,
        bulbExtraRange = 2f..4.5f,
        lengthRange = 0.4f..0.95f,
        cycleSecondsRange = 3f..5f,
        activeFraction = 0.9f,
    ),
)
```

In the shader implementation, two drip slots are selected for detached falling motion when `count >= 2`. Other drips contribute to the edge detail. These values are inputs to the algorithm; they do not imply that every drip falls in the same way.

## Scroll configuration

`MetaballScrollConfig` controls scrolling and banner expansion/collapse behavior.

| Parameter | Default | Description |
|---|---:|---|
| `lockPull` | `70f` | Pull threshold for entering the locked state |
| `unlockOffset` | `1f` | Threshold for returning from the locked state |
| `restEpsilon` | `0.5f` | Rest-state tolerance |
| `settleExpand` | `0.02f` | Settle-completion threshold |
| `calmFadeMillis` | `180` | Calm-state fade duration |
| `pullResistance` | `0.55f` | Pull resistance |
| `pullToOpenEnabled` | `true` | Enables pull-to-open |
| `snapToCollapse` | `true` | Enables snapping to the collapsed position |
| `bannerSpring` | Custom spring | Banner expansion/collapse animation |
| `pullBackSpring` | Medium-low spring | Pull return animation |
| `scrollSpec` | Spring | Scroll animation spec |

Create the state and attach the modifier:

```kotlin
val scroll = rememberMetaballScrollState(
    layout = layout,
    config = MetaballScrollConfig(
        pullToOpenEnabled = true,
        snapToCollapse = true,
        lockPull = 70f,
    ),
)

Column(
    Modifier.metaballScroll(scroll)
) {
    // Scrollable content
}
```

`MetaballScrollState` also exposes the internal `ScrollState` and maintains values such as `isLocked`, `expand`, `calm`, `anchor`, and `scrollY` to coordinate the effect.

## Video integration

The library module does **not** depend on Media3. Video input is provided through this public interface:

```kotlin
interface MetaballVideoSource {
    val frame: Bitmap?
    val ready: Float
    fun setPlaying(playing: Boolean)
}
```

The host application supplies an implementation that exposes the current frame as a `Bitmap`, then passes it to the `video` parameter of `MetaballAvatar`:

```kotlin
MetaballAvatar(
    scroll = scroll,
    layout = layout,
    photo = photo,
    video = videoSource,
)
```

The library calls `setPlaying(true)` when the banner enters its locked state and `setPlaying(false)` when it leaves that state. The `ready` value is used when blending video according to `videoExpandRange`.

### Video implementation in the sample app

The sample app includes `Media3BannerVideo`, an implementation built with:

- `ExoPlayer` for playback.
- `TextureView` as the video surface.
- `Choreographer` to obtain frames.
- `TextureView.getBitmap()` to convert the current frame to a `Bitmap`.
- `Animatable` to fade in the video after a frame is ready.
- `Media3BannerVideo.SurfaceHost()` to attach the `TextureView` to Compose.

This implementation lives in `app/.../Media3BannerVideo.kt`; it is not part of the `metaball` library API. Using it requires the Media3 dependencies included by the sample app.

Example usage in the sample:

```kotlin
val video = rememberMedia3BannerVideo(
    url = videoUrl,
    fadeMillis = 500,
    volume = 0f,
    looping = true,
)

MetaballAvatar(
    scroll = scroll,
    layout = layout,
    photo = photo,
    video = video,
)

// Add the hidden host TextureView to the composition as well:
video?.SurfaceHost()
```

`rememberMedia3BannerVideo` returns `null` when `url == null`. The sample source releases the player when it leaves the composition.

## How it works

The library's high-level rendering flow is:

1. `rememberMetaballLayout` calculates the screen, avatar, banner, and Island geometry.
2. `rememberMetaballScrollState` creates the scroll state and nested-scroll coordination.
3. `metaballScroll` connects user scroll input to that state.
4. `MetaballMotion` calculates the avatar's motion frame from the scroll state, including radius, center, blur, goo, presence, expansion, and blending values.
5. `MetaballAvatar` creates a Compose Canvas and selects the rendering path supported by the Android version.
6. On API 33+, `MetaballShaderRenderer` updates shader uniforms using frame, layout, image, and video data.
7. `MetaballShader` renders the shapes and liquid waves through RuntimeShader.
8. If a video source is attached, its frame is blended with the image over the configured expansion range.

### Shader path

The renderer passes the following information to the shader:

- Avatar and Island dimensions and positions.
- Tint color and effect parameters.
- Image and crop parameters.
- Liquid-flow intensity and animation time.
- Liquify and melt parameters.
- Video dimensions and blending values.

Shader source is generated for each `MetaballDripConfig` and cached internally so that the same configuration does not need to be regenerated repeatedly.

## Android version behavior

- **API 33 and above:** The `RuntimeShader` path is used. It renders the shader effect, procedural drip details, and image/video compositing.
- **API 31 and API 32:** The Canvas-based `FallbackLayer` is used. It draws the avatar shape, Island presence, image crop, and tint using Canvas capabilities.
- **Below API 31:** Not supported by the module's declared `minSdk`.

## Sample app

The `app` module contains a sample profile screen demonstrating two avatar styles:

1. A circular avatar with a size of `120.dp`.
2. A rounded-rectangle avatar sized `168.dp × 136.dp` with a `28.dp` corner radius.

The sample app:

- Loads the artist image with Coil.
- Manages banner video playback with Media3/ExoPlayer.
- Builds the page content from skeleton sections.
- Expands the banner during scrolling and updates status-bar behavior based on the locked state.
- Uses `rememberDisplayCutoutIsland()` to detect the display cutout.
- Provides an internal screen state for switching between circular and rounded-rectangle avatar modes.

To run the sample, open the project in Android Studio and run the `app` configuration on a device or emulator running API 31 or higher. For the full shader rendering path, use API 33 or higher.

## Limitations and notes

- The library does not load image URLs. The host application must provide an `ImageBitmap`.
- The video API is an interface; the Media3 implementation belongs to the sample app.
- The fallback path on API 31 and 32 is not visually equivalent to the RuntimeShader path.
- For correct rendering, the layout, scroll state, and avatar should share the same `MetaballLayout` and `MetaballScrollState` instances.
- Layout and Island values are expressed in dp; shader/motion parameters are mostly `Float` values and may need device-specific tuning.
- When using a custom cutout frame, convert its coordinates and dimensions to dp based on the device density.
- The sample converts video frames to bitmaps using `TextureView.getBitmap()`. This is useful for demonstrating the integration, but it has processing and memory costs and should be profiled on target devices.
- The sample creates its video source with `looping = true`; this is a behavior of the sample implementation, not a requirement of the library interface.

## Build

The project uses Gradle Kotlin DSL and a Gradle Version Catalog.

Versions declared in `gradle/libs.versions.toml` include:

| Tool / dependency | Declared version |
|---|---:|
| Android Gradle Plugin | `9.4.1` |
| Kotlin | `2.4.21` |
| Compose BOM | `2026.09.00` |
| Activity Compose | `1.13.0` |
| Media3 | `1.11.1` |
| Coil | `3.3.0` |
| Dokka | `2.2.0` |
| Maven Publish plugin | `0.37.0` |

Use the project's Gradle wrapper:

```bash
./gradlew :metaball:assembleRelease
./gradlew :app:assembleDebug
```

These commands request the build tasks; this README does not claim that the build was executed successfully as part of documentation generation.

## Publishing and versioning

The project currently defines:

- Library module name: `metaball`.
- Library namespace: `ir.mehdisekoba.metaball`.
- Maven Publish plugin alias in the Version Catalog.

The Maven Central dependency coordinates in the installation examples are placeholders. Before publishing, configure the actual `group`, `artifactId`, and `version`, along with the Maven Central publishing configuration and credentials required by your publishing setup. Once published, replace `YOUR_GROUP_ID` and `1.0.0` in this README with the real coordinates.

The Gradle Version Catalog versions listed above are the versions declared in the supplied project files; they are not the library's Maven artifact version.

## License

No `LICENSE` file was present in the reviewed project files. Add the license you intend to use before public distribution, then update this section with its name and a link to the license file.
