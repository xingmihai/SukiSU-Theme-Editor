@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.xingmihai.sukisu.themeeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.xingmihai.sukisu.themeeditor.ThemeEditorViewModel
import com.xingmihai.sukisu.themeeditor.theme.IMAGE_SLOTS
import com.xingmihai.sukisu.themeeditor.theme.ImageSlot

@Composable
fun ImagesScreen(vm: ThemeEditorViewModel, onPick: (String) -> Unit) {
    val doc = vm.document
    if (doc == null) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("请先在「编辑」页新建或导入主题包")
        }
        return
    }

    var customName by remember { mutableStateOf("") }
    val known = IMAGE_SLOTS.map { it.name }.toSet()
    val extra = doc.images.keys.filter { it !in known }
    val nextSticker = doc.images.keys
        .mapNotNull { Regex("card_sticker_(\\d+)").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
        .maxOrNull()?.plus(1) ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
        item {
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("图片条目", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "条目名固定、无扩展名，内容就是图片的原始字节（WebP / PNG / JPEG）。" +
                            "管理器用 BitmapFactory 校验尺寸；导出时按 ZIP 条目原样写回。" +
                            "v5 新增 bottom_bar_icon_home / superuser / module / setting，" +
                            "普通与自适应主题共用同一组图标。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        IMAGE_SLOTS.groupBy { it.group }.toList().forEach { (group, slots) ->
            item {
                ExpandableGroup(group, "点击展开槽位", initiallyExpanded = group.startsWith("普通")) {
                    slots.forEach { slot ->
                        ImageSlotRow(vm, slot, doc.images[slot.name], onPick)
                    }
                }
            }
        }

        item {
            ExpandableGroup("添加条目", "新增贴纸或写入自定义条目名") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { onPick("card_sticker_$nextSticker") }) {
                        Text("新增贴纸 $nextSticker")
                    }
                }
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("自定义条目名") },
                    placeholder = { Text("例如 background_light / card_sticker_6 / bottom_bar_icon_home") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                FilledTonalButton(
                    onClick = { onPick(customName.trim()) },
                    enabled = customName.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text("选择图片并写入") }
            }
        }

        if (extra.isNotEmpty()) {
            item {
                ExpandableGroup("包内其他条目", "导入时保留的未知条目，导出会原样带回") {
                    extra.forEach { name ->
                        ImageSlotRow(
                            vm,
                            ImageSlot(name, name, ""),
                            doc.images[name],
                            onPick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageSlotRow(
    vm: ThemeEditorViewModel,
    slot: ImageSlot,
    bytes: ByteArray?,
    onPick: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bytes != null) {
            ImagePreview(bytes)
        } else {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(slot.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                slot.name + if (bytes != null) " · ${formatBytes(bytes.size)}" else " · 空",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(onClick = { onPick(slot.name) }) {
            Text(if (bytes == null) "选择" else "替换")
        }
        if (bytes != null) {
            IconButton(onClick = { vm.removeImage(slot.name) }) {
                Icon(Icons.Default.Delete, contentDescription = "移除 ${slot.name}")
            }
        }
    }
}
