package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ThemeManager {
    private const val TAG = "ThemeManager"
    private const val THEME_CONFIG_FILENAME = "theme.json"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val FONT_FILENAME = "font.ttf"

    data class ThemeConfig(
        val isBackgroundEnabled: Boolean,
        val backgroundOpacity: Float,
        val backgroundDim: Float,
        val isFontEnabled: Boolean,
        val customColor: String,
        val homeLayoutStyle: String,
        val nightModeEnabled: Boolean,
        val nightModeFollowSys: Boolean
    )

    suspend fun exportTheme(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_export")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Collect Config
                val prefs = APApplication.sharedPreferences
                val config = ThemeConfig(
                    isBackgroundEnabled = BackgroundConfig.isCustomBackgroundEnabled,
                    backgroundOpacity = BackgroundConfig.customBackgroundOpacity,
                    backgroundDim = BackgroundConfig.customBackgroundDim,
                    isFontEnabled = FontConfig.isCustomFontEnabled,
                    customColor = prefs.getString("custom_color", "blue") ?: "blue",
                    homeLayoutStyle = prefs.getString("home_layout_style", "default") ?: "default",
                    nightModeEnabled = prefs.getBoolean("night_mode_enabled", false),
                    nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", true)
                )

                // 2. Write Config JSON
                val json = JSONObject().apply {
                    put("isBackgroundEnabled", config.isBackgroundEnabled)
                    put("backgroundOpacity", config.backgroundOpacity.toDouble())
                    put("backgroundDim", config.backgroundDim.toDouble())
                    put("isFontEnabled", config.isFontEnabled)
                    put("customColor", config.customColor)
                    put("homeLayoutStyle", config.homeLayoutStyle)
                    put("nightModeEnabled", config.nightModeEnabled)
                    put("nightModeFollowSys", config.nightModeFollowSys)
                }
                File(cacheDir, THEME_CONFIG_FILENAME).writeText(json.toString())

                // 3. Copy Background if enabled
                if (config.isBackgroundEnabled) {
                    val bgFile = File(context.filesDir, "background.jpg") // BackgroundManager.BACKGROUND_FILENAME
                    if (bgFile.exists()) {
                        bgFile.copyTo(File(cacheDir, BACKGROUND_FILENAME))
                    }
                }

                // 4. Copy Font if enabled
                if (config.isFontEnabled) {
                    val fontName = FontConfig.customFontFilename
                    if (fontName != null) {
                        val fontFile = File(context.filesDir, fontName)
                        if (fontFile.exists()) {
                            fontFile.copyTo(File(cacheDir, FONT_FILENAME))
                        }
                    }
                }

                // 5. Zip to Uri
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ZipOutputStream(BufferedOutputStream(os)).use { zos ->
                        cacheDir.listFiles()?.forEach { file ->
                            val entry = ZipEntry(file.name)
                            zos.putNextEntry(entry)
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
    }

    suspend fun importTheme(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_import")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Unzip
                context.contentResolver.openInputStream(uri)?.use { `is` ->
                    ZipInputStream(BufferedInputStream(`is`)).use { zis ->
                        var entry: ZipEntry?
                        while (zis.nextEntry.also { entry = it } != null) {
                            val file = File(cacheDir, entry!!.name)
                            // Prevent path traversal
                            if (!file.canonicalPath.startsWith(cacheDir.canonicalPath)) {
                                continue
                            }
                            FileOutputStream(file).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                }

                // 2. Read Config
                val configFile = File(cacheDir, THEME_CONFIG_FILENAME)
                if (!configFile.exists()) return@withContext false
                
                val json = JSONObject(configFile.readText())
                val isBackgroundEnabled = json.optBoolean("isBackgroundEnabled", false)
                val backgroundOpacity = json.optDouble("backgroundOpacity", 0.5).toFloat()
                val backgroundDim = json.optDouble("backgroundDim", 0.2).toFloat()
                val isFontEnabled = json.optBoolean("isFontEnabled", false)
                val customColor = json.optString("customColor", "blue")
                val homeLayoutStyle = json.optString("homeLayoutStyle", "default")
                val nightModeEnabled = json.optBoolean("nightModeEnabled", false)
                val nightModeFollowSys = json.optBoolean("nightModeFollowSys", true)

                // 3. Apply Background
                BackgroundConfig.setCustomBackgroundOpacityValue(backgroundOpacity)
                BackgroundConfig.setCustomBackgroundDimValue(backgroundDim)
                BackgroundConfig.setCustomBackgroundEnabledState(isBackgroundEnabled)

                if (isBackgroundEnabled) {
                    val bgFile = File(cacheDir, BACKGROUND_FILENAME)
                    if (bgFile.exists()) {
                        val destFile = File(context.filesDir, "background.jpg")
                        bgFile.copyTo(destFile, overwrite = true)
                        // Update URI to point to local file with timestamp to force refresh
                         val fileUri = Uri.fromFile(destFile).buildUpon()
                            .appendQueryParameter("t", System.currentTimeMillis().toString())
                            .build()
                         BackgroundConfig.updateCustomBackgroundUri(fileUri.toString())
                    }
                } else {
                     // Maybe clear if we want to enforce theme state exactly
                     // But user might want to keep files.
                     // The requirement implies importing the theme as is.
                }
                BackgroundConfig.save(context)

                // 4. Apply Font
                if (isFontEnabled) {
                     val fontFile = File(cacheDir, FONT_FILENAME)
                     if (fontFile.exists()) {
                         FontConfig.applyCustomFont(context, fontFile)
                     }
                } else {
                    FontConfig.clearFont(context)
                }
                
                // 5. Apply Color and Home Layout Style
                APApplication.sharedPreferences.edit()
                    .putString("custom_color", customColor)
                    .putString("home_layout_style", homeLayoutStyle)
                    .putBoolean("night_mode_enabled", nightModeEnabled)
                    .putBoolean("night_mode_follow_sys", nightModeFollowSys)
                    .apply()
                
                // 6. Refresh Theme
                refreshTheme.postValue(true)
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
    }
}
