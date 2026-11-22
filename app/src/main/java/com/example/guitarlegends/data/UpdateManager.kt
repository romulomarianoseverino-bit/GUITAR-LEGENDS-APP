package com.example.guitarlegends.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.guitarlegends.model.AppReleaseInfo
import com.example.guitarlegends.model.LocalReleaseCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

sealed class UpdateCheckStatus {
    object Idle : UpdateCheckStatus()
    object Checking : UpdateCheckStatus()
    object Downloading : UpdateCheckStatus()
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
 * Atualizador oficial via GitHub Releases. O Android valida a assinatura do APK
 * durante a instalação; uma assinatura incompatível não substitui o app instalado.
 */
class UpdateManager(private val context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences("guitar_legends_meta", Context.MODE_PRIVATE)
    }

    val installedVersionCode: Int = BuildConfig.VERSION_CODE
    val installedVersionName: String = BuildConfig.VERSION_NAME

    fun getLastSchemaVersion(): Int = prefs.getInt(KEY_SCHEMA_VERSION, 1)
    fun getLastUpdateCheckMillis(): Long = prefs.getLong(KEY_LAST_CHECK_MS, 0L)

    suspend fun runDataMigrationIfNeeded(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentSchema = getLastSchemaVersion()
            if (currentSchema < CURRENT_SCHEMA_VERSION) {
                val audioDir = File(context.filesDir, "imported_audio")
                if (!audioDir.exists()) audioDir.mkdirs()
                val manifest = File(context.filesDir, "library_manifest.json")
                if (manifest.exists()) {
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

    suspend fun fetchLatestGithubRelease(): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = URL("https://api.github.com/repos/romulomarianoseverino-bit/GUITAR-LEGENDS-APP/releases/latest")
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "Guitar-Legends-Android-Updater")
            }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GitHub respondeu HTTP ${connection.responseCode}.")
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name")
            val body = json.optString("body")
            val versionCode = Regex("(?im)^\\s*versionCode\\s*[:=]\\s*(\\d+)\\s*$")
                .find(body)?.groupValues?.get(1)?.toIntOrNull()
                ?: semanticVersionCode(tag)
                ?: throw IllegalStateException("A publicação não informa uma versão numérica válida.")
            val versionName = tag.removePrefix("v").ifBlank { json.optString("name", tag) }
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var checksum = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        checksum = asset.optString("digest").removePrefix("sha256:")
                        break
                    }
                }
            }
            if (apkUrl.isBlank() || !apkUrl.startsWith("https://github.com/")) {
                throw IllegalStateException("A publicação mais recente ainda não contém um APK oficial.")
            }
            Result.success(
                AppReleaseInfo(
                    id = tag,
                    versionCode = versionCode,
                    versionName = versionName,
                    releaseTitle = json.optString("name", "Guitar Legends $versionName"),
                    changelog = body,
                    isMandatory = body.contains("[OBRIGATÓRIA]", ignoreCase = true),
                    downloadUrl = apkUrl,
                    sha256Checksum = checksum,
                    releaseDateLabel = json.optString("published_at").take(10)
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível consultar as atualizações oficiais: ${e.message}"))
        } finally {
            connection?.disconnect()
        }
    }

    private fun semanticVersionCode(tag: String): Int? {
        val match = Regex("^v?(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*$").find(tag) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].ifBlank { "0" }.toIntOrNull() ?: return null
        if (major > 99 || minor > 99 || patch > 99) return null
        return major * 10000 + minor * 100 + patch
    }

    fun evaluateUpdateStatus(releases: List<AppReleaseInfo>): UpdateCheckStatus {
        prefs.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
        val latest = releases.maxByOrNull { it.versionCode } ?: LocalReleaseCatalog.history.first()
        return if (latest.versionCode > installedVersionCode) {
            val mandatory = latest.isMandatory || installedVersionCode < latest.minSupportedVersionCode
            UpdateCheckStatus.UpdateAvailable(installedVersionName, installedVersionCode, latest, mandatory)
        } else {
            UpdateCheckStatus.UpToDate("$installedVersionName (Build $installedVersionCode)", latest)
        }
    }

    suspend fun downloadAndInstall(release: AppReleaseInfo): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            require(release.versionCode > installedVersionCode) { "Esta versão não é mais recente que a instalada." }
            require(release.downloadUrl.startsWith("https://github.com/")) { "Link de atualização não pertence ao GitHub oficial." }
            val folder = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(folder, "guitar-legends-${release.versionCode}.apk")
            connection = (URL(release.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Guitar-Legends-Android-Updater")
            }
            if (connection.responseCode !in 200..299) throw IllegalStateException("Falha no download: HTTP ${connection.responseCode}.")
            connection.inputStream.use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }
            if (!apk.exists() || apk.length() < 1024L) throw IllegalStateException("O arquivo APK baixado está incompleto.")
            if (release.sha256Checksum.isNotBlank()) {
                val actual = MessageDigest.getInstance("SHA-256").digest(apk.readBytes())
                    .joinToString("") { "%02x".format(it) }
                if (!actual.equals(release.sha256Checksum, ignoreCase = true)) {
                    apk.delete()
                    throw SecurityException("A verificação SHA-256 do APK falhou.")
                }
            }
            withContext(Dispatchers.Main) { launchInstaller(apk) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Não foi possível preparar a atualização: ${e.message}"))
        } finally {
            connection?.disconnect()
        }
    }

    private fun launchInstaller(apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            throw IllegalStateException("Autorize a instalação de apps desta fonte nas configurações e toque em Atualizar agora novamente.")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updateprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun verifyChecksum(bytes: ByteArray, expectedSha256: String): Boolean {
        if (expectedSha256.isBlank()) return true
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val hex = digest.joinToString("") { "%02x".format(it) }
        return hex.equals(expectedSha256.trim(), ignoreCase = true)
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        private const val KEY_SCHEMA_VERSION = "schema_version"
        private const val KEY_LAST_CHECK_MS = "last_update_check_ms"
        private const val KEY_LAST_MIGRATED_VERSION_CODE = "last_migrated_version_code"
    }
}
