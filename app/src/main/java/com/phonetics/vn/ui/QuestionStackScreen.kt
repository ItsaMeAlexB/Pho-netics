package com.phonetics.vn.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phonetics.vn.core.Format
import com.phonetics.vn.core.Question
import com.phonetics.vn.core.SubmissionResult

@Composable
fun QuestionStackScreen(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    val q = ui.current

    Column(
        Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(ui.progressLine, style = MaterialTheme.typography.bodySmall)
        if (q == null) {
            Text("No enabled questions. Enable some in Progress, or add more via sync.",
                style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        if (q.emoji.isNotBlank()) Text(q.emoji, style = MaterialTheme.typography.displayMedium)
        Text(q.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(q.component.group.display + " · " + q.component.subgroup + "  [${q.format.name}]",
            style = MaterialTheme.typography.bodySmall)

        if (!ui.answered) {
            QuestionInput(q, ui, vm)
        } else {
            Feedback(ui.feedback)
            Spacer(Modifier.height(4.dp))
            val correct = ui.feedback?.diagnosis?.correct == true
            when {
                correct -> Button(vm::advance, Modifier.fillMaxWidth()) { Text("Next question  ▶") }
                ui.skipped -> Button(vm::advance, Modifier.fillMaxWidth()) { Text("Continue  ▶") }
                else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(vm::tryAgain, Modifier.weight(1f)) { Text("Try again") }
                    Button(vm::advance, Modifier.weight(1f)) { Text("Next  ▶") }
                }
            }
        }
    }
}

@Composable
private fun QuestionInput(q: Question, ui: UiState, vm: AppViewModel) {
    when (q.format) {
        Format.FLASHCARD, Format.AUDIO, Format.ORAL -> {
            if (q.format == Format.AUDIO) Button({ vm.playCurrent() }, Modifier.fillMaxWidth()) { Text("🔊 Play the sound") }
            if (q.format == Format.ORAL) {
                Button({ vm.playCurrent() }, Modifier.fillMaxWidth()) { Text("🔊 Hear the model phrase") }
                MicButton(vm)
            }
            Text("Tap Skip if you don't know it (scored as wrong).",
                style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(vm::skip, Modifier.weight(1f)) { Text("Skip") }
                Button(vm::knownIt, Modifier.weight(1f)) { Text("✓ I knew it") }
            }
        }

        Format.MATCH -> {
            MatchInput(q, vm)
            OutlinedButton(vm::skip, Modifier.fillMaxWidth()) { Text("Skip") }
        }

        Format.PARAGRAPH, Format.TYPE, Format.CLOZE -> {
            TextInput(q, ui, vm)
            OutlinedButton(vm::skip, Modifier.fillMaxWidth()) { Text("Skip") }
        }
    }
}

/** ORAL: record the learner's spoken answer via the platform speech recognizer. */
@Composable
private fun MicButton(vm: AppViewModel) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) vm.startMic() }

    if (ui.micListening) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Button({ vm.stopMic() }, Modifier.weight(1f)) { Text("Stop & grade") }
        }
    } else {
        Button({
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) vm.startMic() else launcher.launch(Manifest.permission.RECORD_AUDIO)
        }, Modifier.fillMaxWidth()) { Text("🎙 Answer by speaking") }
    }
}

@Composable
private fun TextInput(q: Question, ui: UiState, vm: AppViewModel) {
    var text by remember(q.id) { mutableStateOf(ui.typed) }
    val label = when (q.format) {
        Format.PARAGRAPH -> "Answer the question above"
        Format.CLOZE -> "Complete the sentence"
        else -> "Type your answer"
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; vm.onTyped(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        singleLine = true,
    )
    Button({ vm.submit(text) }, Modifier.fillMaxWidth()) { Text("Submit") }
}

/** Two-column matching: tap a Vietnamese word, then its English meaning. */
@Composable
private fun MatchInput(q: Question, vm: AppViewModel) {
    val pairs = q.matchPairs
    if (pairs.isEmpty()) { Text("No pairs defined for this matching question."); return }
    var links by remember(q.id) { mutableStateOf(emptyMap<String, String>()) }
    var selected by remember(q.id) { mutableStateOf<String?>(null) }
    val right = remember(q.id) { pairs.map { it.second }.shuffled() }

    Text("Tap a Vietnamese word, then its English meaning.",
        style = MaterialTheme.typography.bodySmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pairs.forEach { (vn, _) ->
                val linked = links.containsKey(vn)
                if (linked) {
                    Text("✓ $vn", style = MaterialTheme.typography.bodyMedium)
                } else {
                    OutlinedButton({ selected = vn }, Modifier.fillMaxWidth()) {
                        Text(vn + if (selected == vn) " ⬅" else "")
                    }
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            right.forEach { en ->
                val used = links.containsValue(en)
                if (used) {
                    Text("✓ $en", style = MaterialTheme.typography.bodyMedium)
                } else {
                    OutlinedButton({
                        val vn = selected
                        if (vn != null) { links = links + (vn to en); selected = null }
                    }, Modifier.fillMaxWidth()) { Text(en) }
                }
            }
        }
    }
    if (links.isNotEmpty()) {
        links.forEach { (vn, en) -> Text("$vn  →  $en", style = MaterialTheme.typography.bodySmall) }
    }
    if (links.size == pairs.size) {
        Button({ vm.submitMatch(links.toList()) }, Modifier.fillMaxWidth()) { Text("Submit matching") }
    }
}


@Composable
private fun Feedback(res: SubmissionResult?) {
    if (res == null) return
    val d = res.diagnosis
    val title = if (d.correct) "✓ Correct" else "✗ Not quite"
    Text(title, style = MaterialTheme.typography.titleMedium,
        color = if (d.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
    if (!d.correct) {
        if (d.isAmbiguous) {
            Text("Review this one. The intended answer was: ${res.question.targetText}")
        } else {
            Text("Misses: ${d.failures.joinToString(", ") { it.kind.name.lowercase() }}. Answer: ${res.question.targetText}")
        }
    }
}
