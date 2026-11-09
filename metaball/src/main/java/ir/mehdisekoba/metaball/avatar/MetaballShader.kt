package ir.mehdisekoba.metaball.avatar


import ir.mehdisekoba.metaball.util.shaderHash
import java.util.Locale


internal object MetaballShader {
    private val cache = HashMap<MetaballDripConfig, String>()

    fun source(drips: MetaballDripConfig): String =
        synchronized(cache) {
            cache.getOrPut(drips) {
                TEMPLATE.replace(
                    "@@DRIP_CALLS@@",
                    buildDripCalls(drips)
                )
            }
        }

    private fun mix(from: Float, to: Float, amount: Float): Float = from + (to - from) * amount

    private fun mix(range: ClosedFloatingPointRange<Float>, amount: Float): Float =
        mix(range.start, range.endInclusive, amount)

    private fun glslFloat(value: Float): String = String.format(Locale.US, "%.9f", value.toDouble())

    /** Phase offsets (in cycles) of the two droplets that detach and fall. */
    private val FALL_PHASES = floatArrayOf(0.05f, 0.12f)

    private fun buildDripCalls(c: MetaballDripConfig): String {
        // The two drips that detach and fall. The slots are derived from the real drip count
        // (the previous hard-coded indices 2 and 7 never matched index 7 when count == 7,
        // because indices run 0..6, so only ONE droplet ever fell).
        val falling: List<Int> =
            if (c.count >= 2) {
                listOf((c.count * 0.3f).toInt(), (c.count * 0.75f).toInt()).distinct()
            } else {
                emptyList()
            }

        // Both falling drips share one cycle length so the two beads stay in step.
        val fallSeconds = (c.cycleSecondsRange.start + c.cycleSecondsRange.endInclusive) / 2f

        return (0 until c.count).joinToString("\n") { index ->

            val i = index.toFloat()

            val seed = shaderHash(i, 1.7f)

            val neck = mix(c.neckRange, shaderHash(i, 4.1f))

            val bulb = neck + mix(
                c.bulbExtraRange,
                shaderHash(i, 8.3f)
            )

            val slot = (
                    i + 0.5f + (seed - 0.5f) * 0.7f
                    ) / c.count.toFloat()

            val shape = listOf(
                slot,
                neck,
                bulb,
                mix(c.lengthRange, seed)
            ).joinToString(", ") { glslFloat(it) }

            val fallingSlot = falling.indexOf(index)
            val isFalling = fallingSlot >= 0

            val cycle = listOf(
                if (isFalling) 1f else 0f,
                if (isFalling) fallSeconds else mix(c.cycleSecondsRange, shaderHash(i, 11.9f)),
                if (isFalling) FALL_PHASES[fallingSlot.coerceAtMost(FALL_PHASES.size - 1)] else seed * 3.1f,
                i * 1.9f
            ).joinToString(", ") { glslFloat(it) }

            """
        acc = dripAt(
            acc, p, edge, left, width,
            amount, reach, fallDistance,
            t, limit,
            float4($shape),
            float4($cycle)
        );
        """.trimIndent()
        }
    }

    private const val TEMPLATE: String = """
uniform shader uPhoto;
uniform shader uVideo;
uniform float uDensity;
uniform float3 uTint;
uniform float4 uIsland;
uniform float4 uAvatar;
uniform float3 uAvatarExtent;
uniform float4 uEffect;
uniform float2 uFade;
uniform float4 uImage;
uniform float2 uDrop;
uniform float2 uBanner;
uniform float2 uFlow;
uniform float4 uLiquify;
uniform float2 uMelt;
uniform float4 uVideoImage;

float sdBox(float2 p, float2 c, float2 h, float r) {
  float2 q = abs(p - c) - h + float2(r, r);
  return length(max(q, float2(0.0, 0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float smoothUnion(float a, float b, float k) {
  float h = max(k - abs(a - b), 0.0) / max(k, 0.0001);
  return min(a, b) - h * h * h * k * (1.0 / 6.0);
}

float sdDrop(float2 p, float r1, float r2, float h) {
  float2 q = float2(abs(p.x), p.y);
  float b = (r1 - r2) / max(h, 0.0001);
  float a = sqrt(max(1.0 - b * b, 0.0));
  float k = dot(q, float2(-b, a));
  if (k < 0.0) {
    return length(q) - r1;
  }
  if (k > a * h) {
    return length(q - float2(0.0, h)) - r2;
  }
  return dot(q, float2(a, b)) - r1;
}

float hash(float2 p) {
  return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

float2 flowAt(float2 p, float t) {
  float2 q = p;
  q.x += 0.35 * sin(q.y * 1.7 + t * 0.9);
  q.y += 0.30 * sin(q.x * 1.3 - t * 0.7);
  return q - p;
}

float meltAt(float2 uv, float t) {
  float x = uv.x + 0.015 * sin(uv.y * 6.0 + t * 1.1);
  float wave = 0.5 + 0.5 * sin(x * 17.0 + 1.8 * sin(x * 6.0 + t * 0.45) + t * 0.3);
  return wave * wave;
}

float glossAt(float2 p, float2 c, float r) {
  float dist = length(p - c + float2(r * 0.32, r * 0.38));
  return 1.0 - smoothstep(r * 0.08, r * 0.5 + 0.01, dist);
}

// One drip hanging from the banner's bottom edge.
//
//  cycle.x : 1 = this drip detaches a bead that falls, 0 = static drip
//  cycle.y : cycle length in seconds
//  cycle.z : phase offset (in cycles)
//
// Falling drip timeline (s = 0..1 over one cycle):
//   0.00 - 0.22  strand grows out of the edge
//   0.18 - 0.46  strand lengthens, bead swells, neck thins
//   0.46 - 0.60  bead separates, strand retracts into the edge (liquid bridge snaps)
//   0.46 - 0.92  bead falls (accelerating), stretched slightly along its path
//   0.86 - 0.98  bead shrinks away; the cycle restarts
float2 dripAt(
    float2 acc,
    float2 p,
    float edge,
    float left,
    float width,
    float amount,
    float reach,
    float fallDistance,
    float t,
    float limit,
    float4 shape,
    float4 cycle
) {
    float neck = shape.y * amount;
    float bulb = shape.z * amount;
    float x = left + shape.x * width;

    if (abs(p.x - x) > bulb * 2.2 + limit) {
        return acc;
    }

    float root = edge - neck;
    float baseLen = max(reach * amount * shape.w, bulb);

    // Static drip: the original hanging strand with a bulb at the tip.
    if (cycle.x < 0.5) {
        float dStatic = sdDrop(
            p - float2(x, root),
            max(neck, 0.7),
            max(bulb, 0.7),
            max(baseLen, 1.0)
        );
        return float2(min(acc.x, dStatic), acc.y);
    }

    float s = clamp(
    t / max(cycle.y, 0.01) + cycle.z,
    0.0,
    1.0
);

    float grow = smoothstep(0.00, 0.22, s);
    float elong = smoothstep(0.18, 0.46, s);
    float retract = smoothstep(0.46, 0.62, s);

    float lenFull = baseLen * (1.0 + 0.55 * elong);
    float bulbNow = bulb * (1.0 + 0.10 * elong);
    float neckNow = neck * (1.0 - 0.40 * elong);

    // Strand: grows out, then retracts after the bead leaves. Culled only once it is a speck.
    float k = grow * (1.0 - retract);
    float dStrand = 100000.0;
    if (k > 0.02) {
        float r1 = max(neckNow * k, 0.5);
        float r2 = max(bulbNow * max(k, 0.35), 0.5);
        float hh = max(lenFull * k, abs(r1 - r2) * 1.05 + 0.5);
        dStrand = sdDrop(p - float2(x, root), r1, r2, hh);
    }

    // Bead: starts exactly at the strand tip, then falls with gravity.
    float u = clamp((s - 0.46) / 0.46, 0.0, 1.0);
    float fallTravel = clamp(fallDistance * 0.18, 120.0, 180.0);
    float beadR = max(bulbNow, 0.8);
    float beadY = root + lenFull + fallTravel * u * u;
    float shrink = 1.0 - smoothstep(0.86, 0.98, s);
    float beadVisible = step(0.44, s) * shrink;
    float beadStretch = 1.0 + 0.45 * min(u * 2.5, 1.0);

    float2 beadC = float2(x, beadY);
    float2 beadP = float2(p.x, beadC.y + (p.y - beadC.y) / beadStretch);

    float dBead = 100000.0;
    float gloss = 0.0;
    if (beadVisible > 0.001) {
        float r = max(beadR * beadVisible, 0.1);
        dBead = length(beadP - beadC) - r;
        gloss = glossAt(beadP, beadC, r) * beadVisible;
    }

    // Liquid bridge between strand tip and bead: stretches, then snaps.
    float bridge = (1.0 - smoothstep(0.46, 0.58, s)) * bulb * 0.9;
    float dMin = smoothUnion(dStrand, dBead, bridge);

    return float2(
        min(acc.x, dMin),
        max(acc.y, gloss)
    );
}


float2 dripsAt(float2 p, float edge, float left, float width, float amount, float reach, float fallDistance, float t, float limit) {
  float2 acc = float2(100000.0, 0.0);
@@DRIP_CALLS@@
  return float2(acc.x, acc.y * amount);
}

float4 cropAt(float2 image, float aspect, float bias) {
  float2 crop = float2(image.x, image.x / aspect);
  if (image.x / image.y > aspect) {
    crop = float2(image.y * aspect, image.y);
  }
  return float4((image.x - crop.x) * 0.5, (image.y - crop.y) * bias, crop.x, crop.y);
}

float3 photoAt(float2 uv, float spread, float2 lo, float2 size, float spin) {
  float2 safeLo = lo + float2(1.5, 1.5);
  float2 safeHi = lo + size - float2(1.5, 1.5);
  float2 base = lo + uv * size;
  float3 sum = float3(0.0, 0.0, 0.0);
  for (int i = 0; i < 16; i++) {
    float t = (float(i) + 0.5) / 16.0;
    float angle = float(i) * 2.39996323 + spin;
    float2 offset = float2(cos(angle), sin(angle)) * sqrt(t) * spread;
    sum += uPhoto.eval(clamp(base + offset, safeLo, safeHi)).rgb;
  }
  return sum / 16.0;
}

float3 videoAt(float2 uv, float spread, float2 lo, float2 size, float spin) {
  float2 safeLo = lo + float2(1.5, 1.5);
  float2 safeHi = lo + size - float2(1.5, 1.5);
  float2 base = lo + uv * size;
  float3 sum = float3(0.0, 0.0, 0.0);
  for (int i = 0; i < 8; i++) {
    float t = (float(i) + 0.5) / 8.0;
    float angle = float(i) * 2.39996323 + spin;
    float2 offset = float2(cos(angle), sin(angle)) * sqrt(t) * spread;
    sum += uVideo.eval(clamp(base + offset, safeLo, safeHi)).rgb;
  }
  return sum / 8.0;
}

half4 main(float2 fragCoord) {
  float2 pos = fragCoord / uDensity;
  float aa = uEffect.w;
  float presence = uEffect.z;
  float expand = uBanner.x;
  float flow = uFlow.y;
  float radius = uAvatar.z;
  float halfW = uAvatarExtent.x;
  float halfH = uAvatarExtent.y;
  float cornerR = uAvatarExtent.z;

  float top = mix(uAvatar.y - halfH, 0.0, expand);
  float bottom = uAvatar.y + halfH;
  float2 extent = float2(mix(halfW, uBanner.y * 0.5, expand), (bottom - top) * 0.5);
  float2 center = float2(uAvatar.x, (top + bottom) * 0.5);
  float2 local = pos - center;

  float dIsland = sdBox(pos, uIsland.xy, uIsland.zw * presence, uIsland.w * presence) + (1.0 - presence) * 2.0;
  float dShape = sdBox(pos, center, extent, cornerR * (1.0 - expand));
  float gloss = 0.0;


if (flow > 0.0 && pos.y > bottom - 40.0) {

    float edgeWave =
        5.0 * sin(pos.x * 0.105 + uFlow.x * 0.55) +
        1.8 * sin(pos.x * 0.22 - uFlow.x * 0.38) +
        0.9 * sin(pos.x * 0.34 + uFlow.x * 0.27);

    dShape -= flow * edgeWave *
        smoothstep(bottom - 55.0, bottom, pos.y);

    float nearEdge =
        max(aa, uEffect.x * 0.6) + uEffect.y + aa;

    float limit =
        max(min(dShape, nearEdge), 0.0) +
        14.0 * flow + 2.0;

    float2 drips = dripsAt(
        pos,
        bottom,
        center.x - extent.x,
        2.0 * extent.x,
        flow,
        uLiquify.z,
        uLiquify.w,
        uFlow.x,
        limit
    );

    dShape = smoothUnion(
        dShape,
        drips.x,
        12.0 * flow
    );

    gloss = drips.y;
}


  // No permanent hanging drop on the resting avatar. Drips are rendered
  // exclusively by dripsAt() while the pull-to-open liquid flow is active.
  float dAvatar = dShape;

  float d = smoothUnion(dIsland, dAvatar, uEffect.y);
  float coverage = clamp(0.5 - d / aa, 0.0, 1.0);
  if (coverage <= 0.0) {
    return half4(0.0, 0.0, 0.0, 0.0);
  }

  float height = max(bottom - top, 1.0);
  float2 bannerUv = local / (2.0 * extent) + 0.5;
  float3 color = float3(0.0, 0.0, 0.0);
  float tintWeight = 0.0;

  if (radius > 0.5 && uAvatar.w < 1.0) {
    float bannerActive = clamp(max(expand * 3.0, flow * 3.0), 0.0, 1.0);
    float feather = max(aa, uEffect.x * 0.6);
    float edge = clamp(0.5 - dShape / feather, 0.0, 1.0);
    float melt = 1.0;
    if (uImage.w > 0.0 && bannerActive < 0.99) {
      float soft = max(radius * uFade.x, uFade.y);
      float reach = bottom - uIsland.y - uIsland.w + soft;
      float front = mix(-soft, reach, uImage.w);
      melt = smoothstep(front - soft, front + soft, dIsland);
    }
    float collapseWeight = clamp(edge / coverage, 0.0, 1.0) * melt * (1.0 - uAvatar.w);
    float weight = mix(collapseWeight, 1.0, bannerActive);
    // Once the image branch is active, never blend the mask tint over source pixels.
    tintWeight = 1.0;

    if (weight > 0.0) {
      float aspect = extent.x / extent.y;
      float4 photoCrop = cropAt(uImage.xy, aspect, uImage.z);
      float2 lo = photoCrop.xy;
      float2 crop = photoCrop.zw;
      float2 safeLo = lo + float2(1.5, 1.5);
      float2 safeHi = lo + crop - float2(1.5, 1.5);

      float2 uv = bannerUv;
      float dripBlur = 0.0;

      if (flow > 0.0) {
   float meltZone = smoothstep(
    uMelt.x,
    1.0,
    clamp(bannerUv.y, 0.0, 1.0)
);

float overhang = max(bannerUv.y - 1.0, 0.0);

// Extend the image colors downward with the falling liquid.
float fallSpan = max(
    (uLiquify.w * 0.22) / max(2.0 * extent.y, 1.0),
    0.05
);

float dripProgress = clamp(
    overhang / fallSpan,
    0.0,
    1.0
);

float mappedY = bannerUv.y <= 1.0
    ? bannerUv.y
    : 0.985 - dripProgress * 0.28;

        float sides = smoothstep(0.0, 0.06, clamp(bannerUv.x, 0.0, 1.0)) *
                      (1.0 - smoothstep(0.94, 1.0, clamp(bannerUv.x, 0.0, 1.0)));
        float2 shift = flowAt(pos / uLiquify.y, uFlow.x) * (uLiquify.x * 0.35) * flow * meltZone;
        shift.x *= sides;

        uv = float2(clamp(bannerUv.x, 0.01, 0.99), mappedY) + shift / (2.0 * extent);
        uv.y -= meltAt(uv, uFlow.x) * meltZone * meltZone * uMelt.y * flow;
        uv = clamp(uv, float2(0.01, 0.01), float2(0.99, 0.985));

       dripBlur =
    smoothstep(0.94, 1.08, bannerUv.y) *
    (5.0 + 9.0 * flow);
      }

      float texel = crop.y / height;
      float spread = max(uEffect.x, dripBlur) * texel;
      float spin = hash(pos) * 6.2831853;
      float3 sharp = uPhoto.eval(clamp(lo + uv * crop, safeLo, safeHi)).rgb;
      float3 photo = sharp;
      if (spread >= 0.25) {
        float3 blurred = photoAt(uv, spread, lo, crop, spin);
        photo = mix(sharp, blurred, smoothstep(0.25, 3.0, spread));
      }

      if (uVideoImage.w > 0.0) {
        float4 videoCrop = cropAt(uVideoImage.xy, aspect, uVideoImage.z);
        float2 vSafeLo = videoCrop.xy + float2(1.5, 1.5);
        float2 vSafeHi = videoCrop.xy + videoCrop.zw - float2(1.5, 1.5);
        float vSpread = dripBlur * (videoCrop.w / height);
        float3 vSharp = uVideo.eval(clamp(videoCrop.xy + uv * videoCrop.zw, vSafeLo, vSafeHi)).rgb;
        float3 frame = vSpread >= 0.5
          ? mix(vSharp, videoAt(uv, vSpread, videoCrop.xy, videoCrop.zw, spin), smoothstep(0.5, 4.0, vSpread))
          : vSharp;
        photo = mix(photo, frame, uVideoImage.w);
      }
      // Preserve the sampled image/video RGB instead of clipping it to the
      // mask weight (which washed bright pixels toward white during blending).
      color = max(photo, float3(0.0, 0.0, 0.0)) * weight;
      color = min(color + gloss * 0.12 * weight, float3(1.0, 1.0, 1.0));
    }
  }
  color += uTint * (1.0 - tintWeight);
  return half4(color * coverage, coverage);
}
"""
}
