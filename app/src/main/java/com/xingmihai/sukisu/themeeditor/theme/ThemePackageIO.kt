package com.xingmihai.sukisu.themeeditor.theme

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ThemeEntry(
    val name: String,
    val size: Int,
    val compressedSize: Long,
    val method: Int,
)

data class ThemeDocument(
    val json: JSONObject,
    val images: Map<String, ByteArray> = emptyMap(),
)

/** Reads and writes SukiSU-Ultra `.sukisu` packages. */
object ThemePackageIO {

    const val MAX_PACKAGE_BYTES = 128L * 1024 * 1024
    const val MAX_JSON_BYTES = 64L * 1024
    const val MAX_IMAGE_BYTES = 64L * 1024 * 1024
    const val MAX_TOTAL_IMAGE_BYTES = 256L * 1024 * 1024

    // Mirrors ThemePackageStorage version constants (manager main, 2026-11-07).
    const val MIN_SUPPORTED_VERSION = 1
    const val BASE_VERSION = 2
    const val ADAPTIVE_VERSION = 3
    const val APPEARANCE_VERSION = 4
    /** Immersive home layout + custom bottom bar icons. */
    const val HOME_STATUS_VERSION = 5
    const val CURRENT_VERSION = HOME_STATUS_VERSION

    private const val THEME_ENTRY = "theme.json"
    private const val BOTTOM_BAR_ICON_PREFIX = "bottom_bar_icon_"

    /** Stable keys from the manager's BottomBarIconSlot enum; must not be renamed. */
    val BOTTOM_BAR_ICON_SLOTS = listOf("home", "superuser", "module", "setting")

    fun bottomBarIconEntry(slot: String) = BOTTOM_BAR_ICON_PREFIX + slot

    data class ReadResult(
        val document: ThemeDocument,
        val entries: List<ThemeEntry>,
        val name: String,
    )

    fun read(context: Context, uri: Uri): ReadResult {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法打开所选文件")
        if (raw.size > MAX_PACKAGE_BYTES) error("主题包超过 128 MB 上限")

        val plain = ThemePackageCrypto.decrypt(raw)
        val entries = mutableListOf<ThemeEntry>()
        val images = linkedMapOf<String, ByteArray>()
        var jsonText: String? = null
        var totalImages = 0L

        ZipInputStream(ByteArrayInputStream(plain)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                val data = zis.readBytes()
                entries += ThemeEntry(entry.name, data.size, entry.compressedSize, entry.method)
                if (entry.name == THEME_ENTRY) {
                    if (data.size > MAX_JSON_BYTES) error("theme.json 超过 64 KB")
                    jsonText = String(data, Charsets.UTF_8)
                } else {
                    if (data.size > MAX_IMAGE_BYTES) error("条目 ${entry.name} 超过 64 MB")
                    totalImages += data.size
                    if (totalImages > MAX_TOTAL_IMAGE_BYTES) error("附带图片总量超过 256 MB")
                    if (images.containsKey(entry.name)) error("重复条目：${entry.name}")
                    if (!isSafeEntryName(entry.name)) error("非法条目名：${entry.name}")
                    images[entry.name] = data
                }
                zis.closeEntry()
            }
        }

        val text = jsonText ?: error("包内缺少 theme.json")
        val obj = runCatching { JSONObject(text) }
            .getOrElse { error("theme.json 不是合法 JSON：${it.message}") }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "theme.sukisu"
        return ReadResult(ThemeDocument(obj, images), entries, name)
    }

    fun write(context: Context, uri: Uri, doc: ThemeDocument) {
        val problems = validate(doc)
        if (problems.isNotEmpty()) error(problems.joinToString("\n"))

        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { zos ->
            // Match the manager: DEFLATED with level 0 (images are already compressed).
            zos.setLevel(Deflater.NO_COMPRESSION)
            zos.putNextEntry(ZipEntry(THEME_ENTRY))
            zos.write(doc.json.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            doc.images.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }

        val encrypted = ThemePackageCrypto.encrypt(zip.toByteArray())
        if (encrypted.size > MAX_PACKAGE_BYTES) error("加密后主题包超过 128 MB")
        val out = context.contentResolver.openOutputStream(uri) ?: error("无法写入目标文件")
        out.use { it.write(encrypted) }
    }

    private fun isSafeEntryName(name: String): Boolean =
        name.isNotBlank() && !name.startsWith("/") && !name.contains("..") && !name.contains('\\')

    /**
     * Mirrors `ThemePackageStorage.validateVersionedData` + the stream size guards.
     * Unknown JSON fields and unknown ZIP entries are preserved by design, so v5
     * packages round-trip even before their fields were added to the editor UI.
     */
    fun validate(doc: ThemeDocument): List<String> {
        val problems = mutableListOf<String>()
        val version = if (doc.json.has("version")) doc.json.optInt("version", -1) else BASE_VERSION
        if (version !in MIN_SUPPORTED_VERSION..CURRENT_VERSION) {
            problems += "version 必须在 $MIN_SUPPORTED_VERSION..$CURRENT_VERSION（当前 $version）"
        }

        val adaptive = doc.json.optBoolean("adaptive", false)
        if (adaptive && version < ADAPTIVE_VERSION) problems += "adaptive=true 需要格式 v3 及以上"
        if (adaptive) {
            if (doc.json.optJSONObject("light") == null) problems += "自适应主题缺少 light 变体"
            if (doc.json.optJSONObject("dark") == null) problems += "自适应主题缺少 dark 变体"
        }

        val appearance = doc.json.opt("appearance")
        if (appearance != null && appearance !== JSONObject.NULL && version < APPEARANCE_VERSION) {
            problems += "appearance 叠加层需要格式 v4"
        }

        // v5: immersive home requires format 5, on the top level or either variant.
        val usesImmersiveHome = doc.json.optString("homeLayout") == "immersive" ||
            doc.json.optJSONObject("light")?.optString("homeLayout") == "immersive" ||
            doc.json.optJSONObject("dark")?.optString("homeLayout") == "immersive"
        if (usesImmersiveHome && version < HOME_STATUS_VERSION) {
            problems += "沉浸首页（immersive）需要格式 v5"
        }

        // v5: custom bottom bar icons. The manager ignores unknown slots, so only
        // the four stable keys are enforced here.
        val bottomBarIcons = doc.json.optJSONArray("bottomBarIcons")
        if (bottomBarIcons != null) {
            for (i in 0 until bottomBarIcons.length()) {
                val slot = bottomBarIcons.optString(i)
                if (slot in BOTTOM_BAR_ICON_SLOTS && !doc.images.containsKey(bottomBarIconEntry(slot))) {
                    problems += "底栏图标 $slot 缺少图片条目 bottom_bar_icon_$slot"
                }
            }
        }
        val hasBottomBarIconEntries = doc.images.keys.any { name ->
            name.startsWith(BOTTOM_BAR_ICON_PREFIX) &&
                name.removePrefix(BOTTOM_BAR_ICON_PREFIX) in BOTTOM_BAR_ICON_SLOTS
        }
        // The manager bumps to v5 whenever bottom bar icon files are present.
        if (hasBottomBarIconEntries && version < HOME_STATUS_VERSION) {
            problems += "自定义底栏图标需要格式 v5"
        }

        val jsonSize = doc.json.toString().toByteArray(Charsets.UTF_8).size
        if (jsonSize > MAX_JSON_BYTES) problems += "theme.json 超过 64 KB"

        var total = 0L
        doc.images.forEach { (name, bytes) ->
            if (!isSafeEntryName(name)) problems += "非法条目名：$name"
            if (bytes.size > MAX_IMAGE_BYTES) problems += "条目 $name 超过 64 MB"
            total += bytes.size
        }
        if (total > MAX_TOTAL_IMAGE_BYTES) problems += "附带图片总量超过 256 MB"
        return problems
    }

    fun pretty(value: Any?): String = when (value) {
        is JSONObject -> value.toString(2)
        is JSONArray -> value.toString(2)
        null, JSONObject.NULL -> ""
        else -> value.toString()
    }

    fun suggestFileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "SukiSU-theme-$stamp.sukisu"
    }
}
