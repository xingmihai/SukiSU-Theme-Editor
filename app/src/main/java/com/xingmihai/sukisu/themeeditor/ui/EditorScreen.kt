@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.xingmihai.sukisu.themeeditor.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingmihai.sukisu.themeeditor.ThemeEditorViewModel
import com.xingmihai.sukisu.themeeditor.theme.JSON_BLOCKS
import com.xingmihai.sukisu.themeeditor.theme.THEME_GROUPS

@Composable
fun EditorScreen(vm: ThemeEditorViewModel, onNew: () -> Unit, onImport: () -> Unit) {
    if (vm.document == null) {
        EmptyState(onNew = onNew, onImport = onImport)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 104.dp),
    ) {
        item { StatusCard(vm) }

        THEME_GROUPS.forEachIndexed { index, group ->
            item {
                ExpandableGroup(
                    title = group.title,
                    subtitle = group.subtitle,
                    initiallyExpanded = index == 0,
                ) {
                    group.fields.forEach { FieldRow(vm, it) }
                }
            }
        }

        item {
            Text(
                "高级 JSON 字段",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
            )
        }

        JSON_BLOCKS.forEach { block ->
            item { JsonBlockCard(vm, block) }
        }
    }
}
