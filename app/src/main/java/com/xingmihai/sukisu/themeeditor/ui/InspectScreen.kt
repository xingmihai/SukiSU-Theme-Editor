@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.xingmihai.sukisu.themeeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.xingmihai.sukisu.themeeditor.ThemeEditorViewModel

@Composable
fun InspectScreen(vm: ThemeEditorViewModel) {
    val clipboard = LocalClipboardManager.current
    val doc = vm.document
    if (doc == null) {
        Column(Modifier.fillMaxWidth().padding(32.dp)) {
            Text("请先新建或导入主题包")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
        item {
            ElevatedCard(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("包信息", style = MaterialTheme.typography.titleMedium)
                    Text("来源：${vm.sourceName}", style = MaterialTheme.typography.bodySmall)
                    Text("加密：SKTH + AES-256-GCM（IV 12 B，tag 128 bit）",
                        style = MaterialTheme.typography.bodySmall)
                    Text("明文：ZIP，首条目 theme.json，其余为图片原始字节",
                        style = MaterialTheme.typography.bodySmall)
                    Text("条目数：${vm.entries.size} · 图片槽：${doc.images.size}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            ExpandableGroup("ZIP 条目", "导入时读取到的条目清单", initiallyExpanded = true) {
                if (vm.entries.isEmpty()) {
                    Text("新建主题尚未导出，无条目记录", style = MaterialTheme.typography.bodySmall)
                } else {
                    vm.entries.forEach { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(entry.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${formatBytes(entry.size)} · " +
                                    (if (entry.method == 0) "STORED" else "DEFLATED"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            ExpandableGroup("theme.json（格式化）", "可复制，包含所有未识别字段") {
                FilledTonalButton(onClick = {
                    clipboard.setText(AnnotatedString(vm.prettyJson()))
                    vm.notify("已复制 theme.json")
                }) { Text("复制 JSON") }
                Text(
                    vm.prettyJson(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        item {
            ExpandableGroup("格式说明", "与 SukiSU-Ultra 管理器保持一致") {
                Text(
                    "· v1–v5：v3 light/dark 自适应，v4 appearance 叠加层，v5 沉浸首页与自定义底栏图标。\n" +
                        "· v5 新增字段：homeLayout、homeStatusAlignment、homeStatusScale、homePillScale、" +
                        "homeStatusWidthFraction、homeCardImageStyle、showHomeCardImageWithoutBackground、" +
                        "enableProgressiveBlur。\n" +
                        "· v5 新条目 bottom_bar_icon_<slot>，合法 slot：home / superuser / module / setting，" +
                        "普通与自适应主题共用；bottomBarIcons 数组需与图片条目对应。\n" +
                        "· immersive 布局或有底栏图标时必须 v5；旧版管理器会拒绝过高版本，不会只应用一半主题。\n" +
                        "· 整包 ≤128 MB，theme.json ≤64 KB，单图 ≤64 MB，图片总量 ≤256 MB。\n" +
                        "· 密钥硬编码在管理器 APK 中，属于混淆/完整性保护而非版权保护。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
