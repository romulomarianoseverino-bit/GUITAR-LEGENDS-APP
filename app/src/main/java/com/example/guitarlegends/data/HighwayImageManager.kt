package com.example.guitarlegends.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Gerenciador seguro de imagens de fundo/estampa para a Esteira de Notas.
 * Valida cabeçalho (magic bytes), extensão/MIME, tamanho de arquivo e dimensões decodificadas,
 * redimensionando com segurança para a memória da GPU antes de salvar em armazenamento privado.
 */
class HighwayImageManager(private val context: Context) {

    private val storageDir: File by lazy {
        File(context.filesDir, "highway_images").apply { mkdirs() }
    }

    suspend fun importAndValidateHighwayImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri).orEmpty().lowercase()
            if (mimeType.isNotBlank() && mimeType !in ALLOWED_MIME_TYPES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Formato não suportado ($mimeType). Envie uma imagem PNG, JPG ou WEBP.")
                )
            }

            val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    total += read
                    if (total > MAX_FILE_BYTES) {
                        return@withContext Result.failure(
                            IllegalArgumentException("A imagem ultrapassa o limite máximo de 6 MB.")
                        )
                    }
                    out.write(buf, 0, read)
                }
                out.toByteArray()
            } ?: return@withContext Result.failure(
                IllegalArgumentException("Não foi possível ler a imagem selecionada.")
            )

            if (rawBytes.size < 64 || !isValidImageSignature(rawBytes)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Arquivo inválido ou corrompido: assinatura não corresponde a PNG/JPG/WEBP.")
                )
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOpts)
            val width = boundsOpts.outWidth
            val height = boundsOpts.outHeight
            if (width <= 0 || height <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Não foi possível decodificar as dimensões da imagem.")
                )
            }
            if (width < MIN_DIMENSION_PX || height < MIN_DIMENSION_PX) {
                return@withContext Result.failure(
                    IllegalArgumentException("Imagem muito pequena (${width}x$height). Use pelo menos ${MIN_DIMENSION_PX}x${MIN_DIMENSION_PX}px.")
                )
            }
            if (width > MAX_DIMENSION_PX || height > MAX_DIMENSION_PX) {
                return@withContext Result.failure(
                    IllegalArgumentException("Resolução excessiva (${width}x$height). O máximo permitido é ${MAX_DIMENSION_PX}px.")
                )
            }

            val sampleSize = calculateInSampleSize(width, height, TARGET_MAX_WIDTH, TARGET_MAX_HEIGHT)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Falha ao processar imagem selecionada.")
                )

            val scaled = if (decoded.width > TARGET_MAX_WIDTH || decoded.height > TARGET_MAX_HEIGHT) {
                val ratio = minOf(
                    TARGET_MAX_WIDTH.toFloat() / decoded.width,
                    TARGET_MAX_HEIGHT.toFloat() / decoded.height
                )
                val targetW = (decoded.width * ratio).toInt().coerceAtLeast(64)
                val targetH = (decoded.height * ratio).toInt().coerceAtLeast(64)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            // Limpa imagens antigas para não acumular lixo no armazenamento privado
            storageDir.listFiles()?.forEach { old -> runCatching { old.delete() } }

            val destFile = File(storageDir, "custom_highway_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            scaled.recycle()

            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException(e.message ?: "Erro inesperado ao importar imagem para a esteira.")
            )
        }
    }

    fun deleteCustomImage(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val f = File(path)
            if (f.exists() && f.canonicalPath.startsWith(storageDir.canonicalPath)) {
                f.delete()
            }
        }
    }

    companion object {
        private const val MAX_FILE_BYTES = 6 * 1024 * 1024 // 6 MB
        private const val MIN_DIMENSION_PX = 128
        private const val MAX_DIMENSION_PX = 4096
        private const val TARGET_MAX_WIDTH = 512
        private const val TARGET_MAX_HEIGHT = 768
        private val ALLOWED_MIME_TYPES = setOf("image/png", "image/jpeg", "image/jpg", "image/webp")

        fun isValidImageSignature(bytes: ByteArray): Boolean {
            if (bytes.size < 12) return false
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            val isPng = bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            // JPEG: FF D8 FF
            val isJpg = bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
                bytes[2] == 0xFF.toByte()
            // WEBP: RIFF....WEBP
            val isWebp = bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
            return isPng || isJpg || isWebp
        }

        private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
            var inSample = 1
            if (srcH > reqH || srcW > reqW) {
                val halfH = srcH / 2
                val halfW = srcW / 2
                while ((halfH / inSample) >= reqH && (halfW / inSample) >= reqW) {
                    inSample *= 2
                }
            }
            return inSample.coerceAtLeast(1)
        }

        fun drawableForStampId(stampId: String): Int? = when (stampId) {
            "stamp_ps2_flames" -> R.drawable.stamp_ps2_flames_1794942967229
            "stamp_checker_punk" -> R.drawable.stamp_checker_punk_1794942967897
            "stamp_cyber_circuit" -> R.drawable.stamp_cyber_circuit_1794942968138
            "stamp_skull_stage" -> R.drawable.stamp_skull_stage_1794942967970
            else -> null
        }

        fun loadHighwayOverlayBitmap(
            context: Context,
            stampId: String,
            customImagePath: String?
        ): ImageBitmap? {
            if (!customImagePath.isNullOrBlank()) {
                val f = File(customImagePath)
                if (f.exists()) {
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 1
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    val bmp = BitmapFactory.decodeFile(f.absolutePath, opts)
                    if (bmp != null) return bmp.asImageBitmap()
                }
            }
            val resId = drawableForStampId(stampId) ?: return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return runCatching {
                BitmapFactory.decodeResource(context.resources, resId, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
}
