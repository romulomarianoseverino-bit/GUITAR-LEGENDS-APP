package com.example.guitarlegends.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.example.guitarlegends.model.AppReleaseInfo
import com.example.guitarlegends.model.LocalReleaseCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

sealed class UpdateCheckStatus {
    object Idle : UpdateCheckStatus()
    object Checking : UpdateCheckStatus()
    data class UpToDate(val installedVersion: String, val release: AppReleaseInfo) : UpdateCheckStatus()
    data class UpdateAvailable(
        val installedVersionName: String,
        val installedVersionCode: Int,
        val latestRelease: AppReleaseInfo,
        val isMandatory: Boolean
    ) : UpdateCheckStatus()
    data class Error(val message: String) : UpdateCheckStatus()
}

/**
 * Gerenciador Integrado de Versões, Migração de Dados e Atualizações do Aplicativo.
 * - Compara BuildConfig.VERSION_CODE com o catálogo remoto (`/app_releases`) e histórico local.
 * - Executa migração segura de esquema local (`schemaVersion`) preservando músicas e progresso.
 * - Redireciona de forma segura para o canal oficial (Play Store / GitHub Releases HTTPS) via `ACTION_VIEW`
 *   em conformidade com as políticas do Google Play (sem Dynamic Code Loading nem REQUEST_INSTALL_PACKAGES).
 */
class UpdateManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("guitar_legends_meta", Context.MODE_PRIVATE)
    }

    val installedVersionCode: Int = BuildConfig.VERSION_CODE
    val installedVersionName: String = BuildConfig.VERSION_NAME

    fun getLastSchemaVersion(): Int = prefs.getInt(KEY_SCHEMA_VERSION, 1)

    fun getLastUpdateCheckMillis(): Long = prefs.getLong(KEY_LAST_CHECK_MS, 0L)

    /**
     * Garante preservação e migração segura dos dados locais após atualização de versão.
     */
    suspend fun runDataMigrationIfNeeded(): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentSchema = getLastSchemaVersion()
            if (currentSchema < CURRENT_SCHEMA_VERSION) {
                // Garante integridade do manifesto e diretórios sem apagar músicas importadas
                val audioDir = File(context.filesDir, "imported_audio")
                if (!audioDir.exists()) audioDir.mkdirs()

                val manifest = File(context.filesDir, "library_manifest.json")
                if (manifest.exists()) {
                    // Verifica se o JSON continua legível; faz cópia de segurança caso esteja íntegro
                    val text = manifest.readText()
                    if (text.trim().startsWith("[")) {
                        File(context.filesDir, "library_manifest.bak").writeText(text)
                    }
                }
                prefs.edit()
                    .putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION)
                    .putInt(KEY_LAST_MIGRATED_VERSION_CODE, installedVersionCode)
                    .apply()
            }
            Result.success(CURRENT_SCHEMA_VERSION)
        } catch (e: Exception) {
            Result.failure(Exception("Falha na migração segura de dados locais: ${e.message}"))
        }
    }

    fun evaluateUpdateStatus(releases: List<AppReleaseInfo>): UpdateCheckStatus {
        prefs.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
        val sorted = releases.sortedByDescending { it.versionCode }
        val latest = sorted.firstOrNull() ?: LocalReleaseCatalog.history.first()

        return if (latest.versionCode > installedVersionCode) {
            val mandatory = latest.isMandatory || installedVersionCode < latest.minSupportedVersionCode
            UpdateCheckStatus.UpdateAvailable(
                installedVersionName = installedVersionName,
                installedVersionCode = installedVersionCode,
                latestRelease = latest,
                isMandatory = mandatory
            )
        } else {
            UpdateCheckStatus.UpToDate(
                installedVersion = "$installedVersionName (Build $installedVersionCode)",
                release = latest
            )
        }
    }

    fun verifyChecksum(bytes: ByteArray, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(it) }
        return hex.equals(expectedSha256.trim(), ignoreCase = true)
    }

    fun openOfficialUpdateChannel(url: String): Result<Unit> {
        return try {
            val safeUrl = url.takeIf { it.startsWith("https://") }
                ?: "https://github.com/romulomarianoseverino-bit/GUITAR-LEGENDS-APP/releases"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível abrir o canal oficial de atualização: ${e.message}"))
        }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_LAST_CHECK_MS = "last_update_check_ms"
        private const val KEY_LAST_MIGRATED_VERSION_CODE = "last_migrated_version_code"
    }
}
