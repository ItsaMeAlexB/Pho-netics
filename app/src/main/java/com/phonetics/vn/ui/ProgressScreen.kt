package com.phonetics.vn.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val scores by vm.progress.collectAsState()
    val flags by vm.flags.collectAsState()
    val log by vm.syncLog.collectAsState()

    var apiKey by remember { mutableStateOf(vm.apiKey()) }
    var baseUrl by remember { mutableStateOf(vm.state.settings["base_url"] ?: "https://api.deepseek.com") }
    var model by remember { mutableStateOf(vm.state.settings["model"] ?: "deepseek-chat") }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Progress by component", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (scores.isEmpty()) Text("Answer a question to see progress here.")
        scores.forEach { s ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("${s.component.group.display} / ${s.component.subgroup}", fontWeight = FontWeight.SemiBold)
                        Text("correct ${s.correct} / attempts ${s.attempts}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(s.fraction, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Text("Prioritized questions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        vm.state.questions.values.forEach { q ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(q.prompt)
                    Text("${q.component.group.display}/${q.component.subgroup} · ${q.format.name}",
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = q.priority, onCheckedChange = { vm.togglePriority(q.id) })
            }
        }

        if (flags.isNotEmpty()) {
            Text("Flagged", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            flags.forEach { (id, note) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(id, fontWeight = FontWeight.SemiBold)
                        Text(note)
                    }
                }
            }
        }

        Text("DeepSeek sync (optional, text-only)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(apiKey, { apiKey = it; vm.setSetting("api_key", it) },
            label = { Text("API key (masked)") }, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(baseUrl, { baseUrl = it; vm.setSetting("base_url", it) },
            label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(model, { model = it; vm.setSetting("model", it) },
            label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Button({ vm.syncNow() }, Modifier.fillMaxWidth()) { Text("Sync now") }
        log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
