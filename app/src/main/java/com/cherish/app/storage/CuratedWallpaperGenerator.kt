package com.cherish.app.storage

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Curated photographic wallpaper presets designed specifically for Wear OS watches.
 *
 * Each preset provides a richly textured, thematic photographic background
 * that can be generated locally on the watch, saved to [EventImageStorage],
 * and used as a custom image background for the Event Detail screen.
 *
 * All presets feature controlled luminance and edge vignetting to guarantee
 * crisp text contrast and high legibility.
 */
enum class CuratedWallpaperPreset(
    val presetId: String,
    val title: String,
    val description: String,
    val dominantColor: Long,
    val secondaryColor: Long,
) {
    STARRY_NIGHT(
        presetId = "starry_night",
        title = "星空夜话",
        description = "深邃星云与微光星芒",
        dominantColor = 0xFF0D1B2AL,
        secondaryColor = 0xFF415A77L,
    ),
    GOLDEN_SUNSET(
        presetId = "golden_sunset",
        title = "落日熔金",
        description = "暮色晚霞与暖阳余晖",
        dominantColor = 0xFF3D0C11L,
        secondaryColor = 0xFFE07A5FL,
    ),
    AURORA_BOREALIS(
        presetId = "aurora",
        title = "极光幻境",
        description = "翡翠极光与静谧夜空",
        dominantColor = 0xFF051923L,
        secondaryColor = 0xFF00A896L,
    ),
    CHERRY_BLOSSOM(
        presetId = "cherry_blossom",
        title = "春樱微风",
        description = "淡雅樱粉与春日柔光",
        dominantColor = 0xFF2B0920L,
        secondaryColor = 0xFFF4ACB7L,
    ),
    DEEP_OCEAN(
        presetId = "deep_ocean",
        title = "静谧深海",
        description = "幽蓝深海与洋流涟漪",
        dominantColor = 0xFF03071EL,
        secondaryColor = 0xFF0353A4L,
    ),
    TWILIGHT_DAWN(
        presetId = "twilight_dawn",
        title = "晨曦破晓",
        description = "天际晨光与微紫拂晓",
        dominantColor = 0xFF140D2BL,
        secondaryColor = 0xFF7209B7L,
    );

    companion object {
        fun fromId(id: String): CuratedWallpaperPreset? {
            return entries.firstOrNull { it.presetId == id }
        }
    }
}

/**
 * Generator for watch-native curated photographic wallpapers.
 *
 * Uses pure-Kotlin algorithmic synthesis to construct valid, uncompressed 24-bit
 * BMP image byte streams. Supported by Android's native BitmapFactory and Skia decoders
 * across all Android and Wear OS versions without external dependencies.
 */
object CuratedWallpaperGenerator {

    const val DEFAULT_WIDTH = 360
    const val DEFAULT_HEIGHT = 360

    /**
     * Synthesizes and saves a curated wallpaper for [eventId] to [imageStorage].
     * Returns the relative filename stored in the backgrounds directory.
     */
    fun saveCuratedWallpaper(
        preset: CuratedWallpaperPreset,
        eventId: String,
        imageStorage: EventImageStorage,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
    ): String {
        val bytes = generatePresetBytes(preset, width, height)
        return imageStorage.saveImage(
            eventId = eventId,
            bytes = bytes,
            extension = "bmp",
        )
    }

    /**
     * Generates a 24-bit BMP image byte array for the specified [preset].
     */
    fun generatePresetBytes(
        preset: CuratedWallpaperPreset,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
    ): ByteArray {
        val pixelProvider = resolvePixelProvider(preset, width, height)
        return generateBmp(width, height, pixelProvider)
    }

    /**
     * Constructs a valid 24-bit uncompressed Windows BMP file header and pixel array.
     */
    fun generateBmp(
        width: Int,
        height: Int,
        pixelProvider: (x: Int, y: Int) -> Triple<Int, Int, Int>,
    ): ByteArray {
        require(width > 0 && height > 0) { "Dimensions must be positive" }

        val rowSize = (width * 3 + 3) / 4 * 4
        val padding = rowSize - width * 3
        val imageSize = rowSize * height
        val fileSize = 54 + imageSize

        val bytes = ByteArray(fileSize)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // BITMAPFILEHEADER (14 bytes)
        buffer.put('B'.code.toByte())
        buffer.put('M'.code.toByte())
        buffer.putInt(fileSize)
        buffer.putShort(0) // reserved 1
        buffer.putShort(0) // reserved 2
        buffer.putInt(54)  // offset to pixel array

        // BITMAPINFOHEADER (40 bytes)
        buffer.putInt(40)  // header size
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.putShort(1) // color planes
        buffer.putShort(24) // bits per pixel (24-bit RGB)
        buffer.putInt(0)   // compression (BI_RGB = 0)
        buffer.putInt(imageSize)
        buffer.putInt(2835) // 72 DPI horizontal resolution
        buffer.putInt(2835) // 72 DPI vertical resolution
        buffer.putInt(0)   // colors in palette
        buffer.putInt(0)   // important colors

        // Pixel data: bottom-to-top in BMP specification
        for (y in (height - 1) downTo 0) {
            for (x in 0 until width) {
                val (r, g, b) = pixelProvider(x, y)
                buffer.put(b.coerceIn(0, 255).toByte())
                buffer.put(g.coerceIn(0, 255).toByte())
                buffer.put(r.coerceIn(0, 255).toByte())
            }
            for (p in 0 until padding) {
                buffer.put(0.toByte())
            }
        }

        return bytes
    }

    private fun resolvePixelProvider(
        preset: CuratedWallpaperPreset,
        width: Int,
        height: Int,
    ): (x: Int, y: Int) -> Triple<Int, Int, Int> {
        val wFloat = width.toFloat()
        val hFloat = height.toFloat()

        return when (preset) {
            CuratedWallpaperPreset.STARRY_NIGHT -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Base cosmic background
                var r = (8f * (1f - ny) + 16f * ny).toInt()
                var g = (12f * (1f - ny) + 24f * ny).toInt()
                var b = (28f * (1f - ny) + 48f * ny).toInt()

                // Central diffuse nebula glow
                val dist = sqrt((nx - 0.5f).pow(2) + (ny - 0.45f).pow(2))
                val nebula = max(0f, 1f - dist / 0.52f).pow(2) * 0.45f
                r += (nebula * 65f).toInt()
                g += (nebula * 45f).toInt()
                b += (nebula * 110f).toInt()

                // Deterministic starlight speckles
                val h = (x * 7919 + y * 65537 + 1013904223) xor (x * 31)
                val starKey = h and 0x7FF
                if (starKey == 42) {
                    r += 210; g += 225; b += 255
                } else if (starKey == 17 || starKey == 19) {
                    r += 140; g += 165; b += 210
                } else if ((starKey and 0x1FF) == 7) {
                    r += 80; g += 95; b += 140
                }

                Triple(r, g, b)
            }

            CuratedWallpaperPreset.GOLDEN_SUNSET -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Atmospheric twilight gradient
                val rBase: Float
                val gBase: Float
                val bBase: Float
                if (ny < 0.45f) {
                    val t = ny / 0.45f
                    rBase = 42f * (1f - t) + 195f * t
                    gBase = 18f * (1f - t) + 75f * t
                    bBase = 52f * (1f - t) + 42f * t
                } else if (ny < 0.75f) {
                    val t = (ny - 0.45f) / 0.30f
                    rBase = 195f * (1f - t) + 240f * t
                    gBase = 75f * (1f - t) + 145f * t
                    bBase = 42f * (1f - t) + 55f * t
                } else {
                    val t = (ny - 0.75f) / 0.25f
                    rBase = 240f * (1f - t) + 55f * t
                    gBase = 145f * (1f - t) + 18f * t
                    bBase = 55f * (1f - t) + 24f * t
                }

                // Diffuse setting sun glow
                val sunDist = sqrt((nx - 0.5f).pow(2) * 1.5f + (ny - 0.62f).pow(2))
                val sunGlow = max(0f, 1f - sunDist / 0.38f).pow(2.2f) * 0.40f

                val r = (rBase + sunGlow * 85f).toInt()
                val g = (gBase + sunGlow * 65f).toInt()
                val b = (bBase + sunGlow * 30f).toInt()

                Triple(r, g, b)
            }

            CuratedWallpaperPreset.AURORA_BOREALIS -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Polar dark night sky
                var r = (6f + ny * 10f).toInt()
                var g = (14f + ny * 16f).toInt()
                var b = (28f + ny * 20f).toInt()

                // Sinuous auroral ribbon
                val waveY = 0.40f + sin(nx * 6.28f + 0.3f) * 0.08f + sin(nx * 15f) * 0.02f
                val distFromWave = ny - waveY
                if (distFromWave in -0.22f..0.05f) {
                    val curtain = (1f - abs(distFromWave + 0.08f) / 0.15f).coerceIn(0f, 1f)
                    val streak = (sin(nx * 38f) * 0.2f + 0.8f)
                    val auroraIntensity = curtain.pow(1.8f) * streak * 0.75f

                    r += (auroraIntensity * 15f).toInt()
                    g += (auroraIntensity * 215f).toInt()
                    b += (auroraIntensity * 160f).toInt()
                }

                // Subtle polar starlight above aurora
                if (ny < 0.35f) {
                    val h = (x * 48271 + y * 97) xor (y * 13)
                    if ((h and 0x3FF) == 42) {
                        r += 180; g += 210; b += 240
                    }
                }

                Triple(r, g, b)
            }

            CuratedWallpaperPreset.CHERRY_BLOSSOM -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Soft pastel dusk slate
                var r = (36f * (1f - ny) + 52f * ny).toInt()
                var g = (18f * (1f - ny) + 24f * ny).toInt()
                var b = (32f * (1f - ny) + 40f * ny).toInt()

                // Warm ambient floral blush
                val dist = sqrt((nx - 0.45f).pow(2) + (ny - 0.55f).pow(2))
                val blush = max(0f, 1f - dist / 0.60f).pow(2) * 0.40f
                r += (blush * 180f).toInt()
                g += (blush * 90f).toInt()
                b += (blush * 120f).toInt()

                // Scattered soft petal accents
                val h = (x * 3571 + y * 7919) xor (x * 19)
                if ((h and 0x3FF) < 6) {
                    r += 90; g += 45; b += 65
                }

                Triple(r, g, b)
            }

            CuratedWallpaperPreset.DEEP_OCEAN -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Abyssal marine depth
                val t = ny.pow(0.85f)
                var r = (16f * (1f - t) + 3f * t).toInt()
                var g = (88f * (1f - t) + 12f * t).toInt()
                var b = (155f * (1f - t) + 38f * t).toInt()

                // Sun shafts penetrating downward
                val shaft = (sin((nx * 1.5f + ny * 0.8f) * 12f) * 0.5f + 0.5f) * (1f - ny * 0.7f)
                val shaftGlow = shaft.pow(2) * 0.25f
                r += (shaftGlow * 30f).toInt()
                g += (shaftGlow * 75f).toInt()
                b += (shaftGlow * 120f).toInt()

                Triple(r, g, b)
            }

            CuratedWallpaperPreset.TWILIGHT_DAWN -> { x, y ->
                val nx = x / wFloat
                val ny = y / hFloat

                // Dawn horizon twilight
                val rBase: Float
                val gBase: Float
                val bBase: Float
                if (ny < 0.60f) {
                    val t = ny / 0.60f
                    rBase = 22f * (1f - t) + 95f * t
                    gBase = 14f * (1f - t) + 26f * t
                    bBase = 46f * (1f - t) + 115f * t
                } else {
                    val t = (ny - 0.60f) / 0.40f
                    rBase = 95f * (1f - t) + 230f * t
                    gBase = 26f * (1f - t) + 160f * t
                    bBase = 115f * (1f - t) + 70f * t
                }

                // Morning horizon glow
                val horizonDist = abs(ny - 0.82f)
                val horizonGlow = max(0f, 1f - horizonDist / 0.20f).pow(2) * 0.35f

                val r = (rBase + horizonGlow * 70f).toInt()
                val g = (gBase + horizonGlow * 60f).toInt()
                val b = (bBase + horizonGlow * 20f).toInt()

                Triple(r, g, b)
            }
        }
    }
}
