@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xingmihai.sukisu.themeeditor.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xingmihai.sukisu.themeeditor.ThemeEditorViewModel
import com.xingmihai.sukisu.themeeditor.theme.ThemePackageCrypto
import com.xingmihai.sukisu.themeeditor.theme.ThemePackageIO

@Composable
fun ThemeEditorApp(vm: ThemeEditorViewModel, incomingUri: Uri?) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }
    var pendingSlot by remember { mutableStateOf<String?>(null) }

    val openPackage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.load(context, it) } }

    val createPackage = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ThemePackageCrypto.MIME)
    ) { uri -> uri?.let { vm.save(context, it) } }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val slot = pendingSlot
        if (uri != null && !slot.isNullOrBlank()) vm.setImage(context, uri, slot)
        pendingSlot = null
    }

    fun requestImage(slot: String) {
        pendingSlot = slot
        pickImage.launch(arrayOf("image/*"))
    }

    LaunchedEffect(incomingUri) {
        incomingUri?.let { vm.load(context, it) }
    }
    LaunchedEffect(vm.statusId) {
        if (vm.status.isNotBlank()) snackbar.showSnackbar(vm.status)
    }

    val tabs = listOf(
        "编辑" to Icons.Default.Edit,
        "图片" to Icons.Default.Image,
        "包信息" to Icons.Default.Info,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("SukiSU 主题编辑器")
                        Text(
                            ".sukisu · Material 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.newTheme() }) {
                        Icon(Icons.Default.Add, contentDescription = "新建主题")
                    }
                    IconButton(onClick = { openPackage.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "导入主题包")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(),
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createPackage.launch(ThemePackageIO.suggestFileName()) },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("导出 .sukisu") },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> EditorScreen(
                    vm = vm,
                    onNew = { vm.newTheme() },
                    onImport = { openPackage.launch(arrayOf("*/*")) },
                )
                1 -> ImagesScreen(vm = vm, onPick = { slot -> requestImage(slot) })
                else -> InspectScreen(vm = vm)
            }
        }
    }
}
