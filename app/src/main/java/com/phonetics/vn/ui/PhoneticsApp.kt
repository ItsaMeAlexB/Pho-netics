package com.phonetics.vn.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phonetics.vn.ui.theme.PhoneticsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneticsApp(vm: AppViewModel = viewModel()) {
    val screen by vm.screen.collectAsState()
    PhoneticsTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Phở-netics") },
                    actions = {
                        TextButton(onClick = {
                            vm.show(if (screen == AppScreen.STACK) AppScreen.PROGRESS else AppScreen.STACK)
                        }) {
                            Text(if (screen == AppScreen.STACK) "Progress" else "Back")
                        }
                    },
                )
            },
        ) { pad ->
            when (screen) {
                AppScreen.STACK -> QuestionStackScreen(vm)
                AppScreen.PROGRESS -> ProgressScreen(vm)
            }
        }
    }
}
