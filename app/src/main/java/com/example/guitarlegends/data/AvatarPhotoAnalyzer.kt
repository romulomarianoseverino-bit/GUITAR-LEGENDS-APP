package com.example.guitarlegends.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.example.guitarlegends.model.StudioCatalog
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class AvatarPhotoAnalysisResult(
    val savedPathOrUri: String,
    val skinToneHex: Long,
    val hairColorHex: Long,
    val extractedFromPixels: Boolean
)

/**
 * Analisa localmente a foto escolhida via Android Photo Picker (sem permissões amplas de armazenamento),
 * salva uma miniatura privada em JPEG e extrai paleta sugerida de tom de pele e cabelo.
 */
object AvatarPhotoAnalyzer {

    fun analyzeAndCachePhoto(context: Context, uriString: String): AvatarPhotoAnalysisResult {
        val defaultSkin = StudioCatalog.skinTones[1]
        val defaultHair = StudioCatalog.paletteColors[0]
        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return AvatarPhotoAnalysisResult(uriString, defaultSkin, defaultHair, false)

        return try {
            val bitmap = decodeSampledBitmap(context, uri, maxDim = 256)
                ?: return AvatarPhotoAnalysisResult(uriString, defaultSkin, defaultHair, false)

            val savedPath = saveThumbnailToAppStorage(context, bitmap) ?: uriString
            val (skinHex, hairHex) = extractSkinAndHairPalette(bitmap)
            bitmap.recycle()

            AvatarPhotoAnalysisResult(
                savedPathOrUri = savedPath,
                skinToneHex = skinHex,
                hairColorHex = hairHex,
                extractedFromPixels = true
            )
        } catch (_: Throwable) {
            AvatarPhotoAnalysisResult(uriString, defaultSkin, defaultHair, false)
        }
    }

    private const val MAX_AVATAR_BYTES = 10 * 1024 * 1024

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buf)
                if (read <= 0) break
                total += read
                if (total > MAX_AVATAR_BYTES) return null
                out.write(buf, 0, read)
            }
            out.toByteArray()
        } ?: return null

        if (rawBytes.size < 32 || !hasKnownImageHeader(rawBytes)) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var inSampleSize = 1
        while ((bounds.outWidth / inSampleSize) > maxDim * 2 || (bounds.outHeight / inSampleSize) > maxDim * 2) {
            inSampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null

        val scale = min(1f, maxDim.toFloat() / max(decoded.width, decoded.height).toFloat())
        return if (scale < 0.99f) {
            val w = max(1, (decoded.width * scale).toInt())
            val h = max(1, (decoded.height * scale).toInt())
            val scaled = Bitmap.createScaledBitmap(decoded, w, h, true)
            if (scaled !== decoded) decoded.recycle()
            scaled
        } else {
            decoded
        }
    }

    private fun hasKnownImageHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        val isJpeg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        val isWebp = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        val isGif = bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()
        val isHeif = bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()
        return isPng || isJpeg || isWebp || isGif || isHeif
    }

    private fun saveThumbnailToAppStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "avatar_photos").apply {
                if (!exists()) mkdirs()
            }
            // Mantém somente as miniaturas mais recentes para não acumular arquivos
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(4)?.forEach { old ->
                runCatching { old.delete() }
            }
            val outFile = File(dir, "avatar_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            outFile.absolutePath
        } catch (_: Throwable) {
            null
        }
    }

    internal fun extractSkinAndHairPalette(bitmap: Bitmap): Pair<Long, Long> {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return StudioCatalog.skinTones[1] to StudioCatalog.paletteColors[0]

        // Centro facial (25%..75% horizontal, 30%..70% vertical) -> tom de pele médio
        var skinR = 0L
        var skinG = 0L
        var skinB = 0L
        var skinCount = 0

        val xStart = (w * 0.25f).toInt().coerceIn(0, w - 1)
        val xEnd = (w * 0.75f).toInt().coerceIn(xStart + 1, w)
        val yStartSkin = (h * 0.30f).toInt().coerceIn(0, h - 1)
        val yEndSkin = (h * 0.70f).toInt().coerceIn(yStartSkin + 1, h)

        val stepX = max(1, (xEnd - xStart) / 16)
        val stepY = max(1, (yEndSkin - yStartSkin) / 16)

        for (y in yStartSkin until yEndSkin step stepY) {
            for (x in xStart until xEnd step stepX) {
                val px = bitmap.getPixel(x, y)
                if (Color.alpha(px) < 128) continue
                skinR += Color.red(px)
                skinG += Color.green(px)
                skinB += Color.blue(px)
                skinCount++
            }
        }

        val nearestSkin = if (skinCount > 0) {
            nearestColorInPalette(
                (skinR / skinCount).toInt(),
                (skinG / skinCount).toInt(),
                (skinB / skinCount).toInt(),
                StudioCatalog.skinTones
            )
        } else {
            StudioCatalog.skinTones[1]
        }

        // Região superior (5%..32% vertical) -> cor predominante/vibrante do cabelo
        var bestHairR = 31
        var bestHairG = 27
        var bestHairB = 38
        var topWeight = -1f
        var hairSumR = 0L
        var hairSumG = 0L
        var hairSumB = 0L
        var hairCount = 0

        val yEndHair = (h * 0.32f).toInt().coerceIn(1, h)
        val hairStepY = max(1, yEndHair / 12)
        for (y in 0 until yEndHair step hairStepY) {
            for (x in xStart until xEnd step stepX) {
                val px = bitmap.getPixel(x, y)
                if (Color.alpha(px) < 128) continue
                val r = Color.red(px)
                val g = Color.green(px)
                val b = Color.blue(px)
                hairSumR += r
                hairSumG += g
                hairSumB += b
                hairCount++

                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val chroma = (maxC - minC).toFloat()
                if (chroma > topWeight) {
                    topWeight = chroma
                    bestHairR = r
                    bestHairG = g
                    bestHairB = b
                }
            }
        }

        val nearestHair = if (hairCount > 0) {
            if (topWeight >= 42f) {
                nearestColorInPalette(bestHairR, bestHairG, bestHairB, StudioCatalog.paletteColors)
            } else {
                nearestColorInPalette(
                    (hairSumR / hairCount).toInt(),
                    (hairSumG / hairCount).toInt(),
                    (hairSumB / hairCount).toInt(),
                    StudioCatalog.paletteColors
                )
            }
        } else {
            StudioCatalog.paletteColors[0]
        }

        return nearestSkin to nearestHair
    }

    private fun nearestColorInPalette(r: Int, g: Int, b: Int, palette: List<Long>): Long {
        var best = palette.first()
        var bestDist = Long.MAX_VALUE
        for (hex in palette) {
            val pr = ((hex shr 16) and 0xFF).toInt()
            val pg = ((hex shr 8) and 0xFF).toInt()
            val pb = (hex and 0xFF).toInt()
            val dr = (r - pr).toLong()
            val dg = (g - pg).toLong()
            val db = (b - pb).toLong()
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                best = hex
            }
        }
        return best
    }
}
