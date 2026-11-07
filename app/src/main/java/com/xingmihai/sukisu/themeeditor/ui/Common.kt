@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.xingmihai.sukisu.themeeditor.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingmihai.sukisu.themeeditor.ThemeEditorViewModel
import com.xingmihai.sukisu.themeeditor.theme.FieldSpec
import com.xingmihai.sukisu.themeeditor.theme.JsonBlock
import com.xingmihai.sukisu.themeeditor.theme.Kind

fun formatBytes(size: Int): String = when {
    size >= 1024 * 1024 -> "%.1f MB".format(size / 1024f / 1024f)
    size >= 1024 -> "%.1f KB".format(size / 1024f)
    else -> "$size B"
}

@Composable
fun ExpandableGroup(
    title: String,
    subtitle: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val corner by animateDpAsState(
        targetValue = if (expanded) 28.dp else 20.dp,
        label = "shapeMorph",
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.elevatedCardColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开",
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun StatusCard(vm: ThemeEditorViewModel) {
    val doc = vm.document ?: return
    val version = if (doc.json.has("version")) doc.json.optInt("version", -1) else 2
    val adaptive = doc.json.optBoolean("adaptive", false)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (vm.errors.isEmpty())
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(vm.sourceName, style = MaterialTheme.typography.titleMedium)
            Text(
                "格式 v$version · ${if (adaptive) "自适应" else "普通"} · ${doc.images.size} 个图片条目",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (vm.errors.isEmpty()) {
                Text(
                    "校验通过，可以导出",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                vm.errors.forEach {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                if (vm.errors.any { it.contains("v5") }) {
                    FilledTonalButton(
                        onClick = { vm.upgradeToV5() },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text("一键升级到 v5") }
                }
            }
        }
    }
}

@Composable
private fun FreeEdit(
    vm: ThemeEditorViewModel,
    spec: FieldSpec,
    initial: String,
    keyboard: KeyboardType,
    convert: (String) -> Any?,
) {
    var text by remember(vm.epoch, spec.key) { mutableStateOf(initial) }
    var bad by remember(vm.epoch, spec.key) { mutableStateOf(false) }
    val support: (@Composable () -> Unit)? =
        if (bad) { { Text("格式不正确") } } else null

    OutlinedTextField(
        value = text,
        onValueChange = { t ->
            text = t
            if (t.isBlank()) {
                bad = false
                vm.update(spec.key, null)
            } else {
                val value = runCatching { convert(t) }.getOrNull()
                if (value == null) {
                    bad = true
                } else {
                    bad = false
                    vm.update(spec.key, value)
                }
            }
        },
        label = { Text(spec.label) },
        singleLine = true,
        isError = bad,
        supportingText = support,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun FieldRow(vm: ThemeEditorViewModel, spec: FieldSpec) {
    val current = vm.current(spec.key)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        when (spec.kind) {
            Kind.BOOL -> Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(spec.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = current as? Boolean ?: false,
                    onCheckedChange = { vm.update(spec.key, it) },
                )
            }

            Kind.CHOICE -> {
                Text(spec.label, style = MaterialTheme.typography.labelLarge)
                if (spec.choices.size in 2..4) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        spec.choices.forEachIndexed { index, choice ->
                            SegmentedButton(
                                selected = current == choice.value,
                                onClick = { vm.update(spec.key, choice.value) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = spec.choices.size,
                                ),
                            ) { Text(choice.label, maxLines = 1) }
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        spec.choices.forEach { choice ->
                            FilterChip(
                                selected = current == choice.value,
                                onClick = { vm.update(spec.key, choice.value) },
                                label = { Text(choice.label) },
                            )
                        }
                    }
                }
            }

            Kind.PERCENT -> {
                val value = (current as? Number)?.toFloat() ?: spec.min
                Text(
                    "${spec.label}：${value.toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = value.coerceIn(spec.min, spec.max),
                    onValueChange = { vm.update(spec.key, it.toInt()) },
                    valueRange = spec.min..spec.max,
                )
            }

            Kind.FLOAT -> {
                val value = (current as? Number)?.toFloat() ?: 1f
                Text(
                    "${spec.label}：%.2f".format(value),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = value.coerceIn(spec.min, spec.max),
                    onValueChange = { vm.update(spec.key, it) },
                    valueRange = spec.min..spec.max,
                )
            }

            Kind.INT -> FreeEdit(
                vm, spec,
                initial = current?.toString() ?: "",
                keyboard = KeyboardType.Number,
                convert = { it.toIntOrNull() },
            )

            Kind.STRING -> FreeEdit(
                vm, spec,
                initial = current?.toString() ?: "",
                keyboard = KeyboardType.Text,
                convert = { it },
            )

            Kind.COLOR -> {
                val argb = (current as? Number)?.toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (argb != null) {
                        Box(
                            Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(argb))
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        FreeEdit(
                            vm, spec,
                            initial = argb?.let { "#%08X".format(it) } ?: "",
                            keyboard = KeyboardType.Ascii,
                            convert = { android.graphics.Color.parseColor(it) },
                        )
                    }
                }
            }
        }

        if (spec.hint.isNotEmpty() || current == null) {
            Text(
                (if (current == null) "未设置 · " else "") + spec.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun JsonBlockCard(vm: ThemeEditorViewModel, block: JsonBlock) {
    var text by remember(vm.epoch, block.key) { mutableStateOf(vm.rawBlock(block.key)) }
    var error by remember(vm.epoch, block.key) { mutableStateOf<String?>(null) }

    ExpandableGroup(block.title, block.subtitle) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; error = null },
            label = { Text("${block.key} · ${if (block.isArray) "JSON 数组" else "JSON 对象"}") },
            minLines = 4,
            maxLines = 14,
            isError = error != null,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = { error = vm.setRawJson(block.key, text, block.isArray) }) {
                Text("应用")
            }
            OutlinedButton(onClick = {
                vm.removeRawJson(block.key)
                text = ""
                error = null
            }) { Text("移除字段") }
            TextButton(onClick = { text = vm.prettyBlock(block.key); error = null }) {
                Text("格式化")
            }
        }
    }
}

@Composable
fun EmptyState(onNew: () -> Unit, onImport: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("还没有打开主题包", style = MaterialTheme.typography.titleLarge)
        Text(
            "新建一个 v5 空主题（旧版管理器可在格式版本处切回 v4 或更低），或从文件管理器导入 .sukisu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onNew, modifier = Modifier.padding(top = 8.dp)) {
            Text("新建 v5 空主题")
        }
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Text("导入 .sukisu", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun ImagePreview(bytes: ByteArray, modifier: Modifier = Modifier) {
    val bitmap = remember(bytes) {
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
            .getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    } else {
        Box(
            modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("?", style = MaterialTheme.typography.bodySmall)
        }
    }
}
