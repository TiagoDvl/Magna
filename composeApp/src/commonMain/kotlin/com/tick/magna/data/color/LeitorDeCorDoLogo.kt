package com.tick.magna.data.color

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.max

/**
 * Reads a party's colour out of its logo.
 *
 * Behind an interface because it is the one step of the sync that needs an image decoder, and
 * a test that is about what the sync stores should not have to hand it a real GIF.
 */
internal interface LeitorDeCorDoLogoInterface {

    /** The logo's dominant colour as ARGB, or null when the bytes are not an image the
     * platform can read, or are an image with no colour in it. */
    fun corDe(bytes: ByteArray): Int?
}

internal class LeitorDeCorDoLogo : LeitorDeCorDoLogoInterface {

    /**
     * Decodes, samples, counts.
     *
     * The sampling is a stride rather than a scale: every party logo measured is a few hundred
     * pixels on a side, so this reads every pixel of all of them, and the cap is only there so
     * that the day the register starts serving something big this stays a fixed cost.
     *
     * A failure here is a party without a colour, not a failed sync. The register serves these
     * as GIF and twelve of the twenty-seven URLs it publishes are 404 in the first place, so
     * "no colour" is an ordinary outcome that the rest of the app already handles.
     */
    override fun corDe(bytes: ByteArray): Int? {
        if (bytes.isEmpty()) return null

        return try {
            val bitmap = bytes.decodeToImageBitmap()
            if (bitmap.width <= 0 || bitmap.height <= 0) return null

            val pixels = bitmap.toPixelMap()
            val passoX = max(1, bitmap.width / LADO_DA_AMOSTRA)
            val passoY = max(1, bitmap.height / LADO_DA_AMOSTRA)

            val amostra = ArrayList<Int>(
                (bitmap.width / passoX + 1) * (bitmap.height / passoY + 1),
            )

            var y = 0
            while (y < bitmap.height) {
                var x = 0
                while (x < bitmap.width) {
                    amostra += pixels[x, y].toArgb()
                    x += passoX
                }
                y += passoY
            }

            corDominante(amostra.toIntArray())
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        /** At most this many samples per side, so a big image costs the same as a small one. */
        const val LADO_DA_AMOSTRA = 160
    }
}
