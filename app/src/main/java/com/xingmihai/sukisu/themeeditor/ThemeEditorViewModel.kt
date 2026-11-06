package com.xingmihai.sukisu.themeeditor

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingmihai.sukisu.themeeditor.theme.ThemeDocument
import com.xingmihai.sukisu.themeeditor.theme.ThemeEntry
import com.xingmihai.sukisu.themeeditor.theme.ThemePackageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ThemeEditorViewModel : ViewModel() {

    var document by mutableStateOf<ThemeDocument?>(null)
        private set
    var entries by mutableStateOf<List<ThemeEntry>>(emptyList())
        private set
    var errors by mutableStateOf<List<String>>(emptyList())
        private set
    var sourceName by mutableStateOf("未打开主题包")
        private set
    var epoch by mutableIntStateOf(0)
        private set
    var status by mutableStateOf("")
        private set
    var statusId by mutableIntStateOf(0)
        private set

    fun notify(message: String) {
        status = message
        statusId++
    }

    fun newTheme() {
        document = ThemeDocument(JSONObject().apply {
            put("version", 4)
            put("adaptive", false)
            put("themeMode", 0)
            put("uiMode", "MATERIAL")
            put("miuixMonet", true)
        })
        entries = emptyList()
        errors = ThemePackageIO.validate(document!!)
        sourceName = "新建主题（未保存）"
        epoch++
        notify("已新建 v4 空主题")
    }

    fun load(context: Context, uri: Uri) = viewModelScope.launch {
        val app = context.applicationContext
        val result = runCatching {
            withContext(Dispatchers.IO) { ThemePackageIO.read(app, uri) }
        }
        withContext(Dispatchers.Main) {
            result.onSuccess { r ->
                document = r.document
                entries = r.entries
                errors = ThemePackageIO.validate(r.document)
                sourceName = r.name
                epoch++
                notify("已导入 ${r.name}（${r.entries.size} 个条目）")
            }
            result.onFailure { notify("导入失败：${it.message}") }
        }
    }

    fun save(context: Context, uri: Uri) = viewModelScope.launch {
        val doc = document
        if (doc == null) {
            notify("请先新建或导入主题包")
            return@launch
        }
        val problems = ThemePackageIO.validate(doc)
        if (problems.isNotEmpty()) {
            notify("校验未通过：${problems.first()}")
            return@launch
        }
        val app = context.applicationContext
        val result = runCatching {
            withContext(Dispatchers.IO) { ThemePackageIO.write(app, uri, doc) }
        }
        withContext(Dispatchers.Main) {
            result.onSuccess { notify("导出成功") }
                .onFailure { notify("导出失败：${it.message}") }
        }
    }

    /** Keeps every unknown JSON field by copying the whole object. Value == null removes it. */
    fun update(key: String, value: Any?) {
        val doc = document ?: return
        val next = JSONObject(doc.json.toString())
        if (value == null) next.remove(key) else next.put(key, value)
        val updated = doc.copy(json = next)
        document = updated
        errors = ThemePackageIO.validate(updated)
    }

    fun current(key: String): Any? {
        val v = document?.json?.opt(key) ?: return null
        return if (v === JSONObject.NULL) null else v
    }

    fun rawBlock(key: String): String = ThemePackageIO.pretty(current(key))

    fun prettyBlock(key: String): String = rawBlock(key)

    /** Returns null on success, otherwise an error message. */
    fun setRawJson(key: String, text: String, asArray: Boolean): String? {
        val doc = document ?: return "没有打开的主题包"
        if (text.isBlank()) {
            update(key, null)
            return null
        }
        return try {
            val parsed: Any = if (asArray) JSONArray(text) else JSONObject(text)
            val next = JSONObject(doc.json.toString())
            next.put(key, parsed)
            val updated = doc.copy(json = next)
            document = updated
            errors = ThemePackageIO.validate(updated)
            null
        } catch (e: Exception) {
            "JSON 解析失败：${e.message}"
        }
    }

    fun removeRawJson(key: String) = update(key, null)

    fun setImage(context: Context, uri: Uri, slot: String) = viewModelScope.launch {
        val app = context.applicationContext
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取图片")
                if (bytes.size > ThemePackageIO.MAX_IMAGE_BYTES) error("图片超过 64 MB")
                bytes
            }
        }
        withContext(Dispatchers.Main) {
            result.onSuccess { bytes ->
                val doc = document
                if (doc != null) {
                    val map = LinkedHashMap(doc.images).apply { put(slot, bytes) }
                    val updated = doc.copy(images = map)
                    document = updated
                    errors = ThemePackageIO.validate(updated)
                    notify("已替换 $slot")
                }
            }.onFailure { notify("图片读取失败：${it.message}") }
        }
    }

    fun removeImage(slot: String) {
        val doc = document ?: return
        val map = LinkedHashMap(doc.images).apply { remove(slot) }
        val updated = doc.copy(images = map)
        document = updated
        errors = ThemePackageIO.validate(updated)
        notify("已移除 $slot")
    }

    fun prettyJson(): String = ThemePackageIO.pretty(document?.json)
}
