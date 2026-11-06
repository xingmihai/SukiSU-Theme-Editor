package com.xingmihai.sukisu.themeeditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xingmihai.sukisu.themeeditor.ui.SukiSUTheme
import com.xingmihai.sukisu.themeeditor.ui.ThemeEditorApp

class MainActivity : ComponentActivity() {

    private val incoming = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incoming.value = intent?.data
        setContent {
            val vm: ThemeEditorViewModel = viewModel()
            SukiSUTheme {
                ThemeEditorApp(vm = vm, incomingUri = incoming.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incoming.value = intent.data
    }
}
