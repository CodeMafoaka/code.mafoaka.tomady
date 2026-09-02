package com.tomady.nutrition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomady.nutrition.ui.CURRENT_USER_ID
import com.tomady.nutrition.ui.components.AiStatusPillPresets
import com.tomady.nutrition.ui.components.SectionCard
import com.tomady.nutrition.ui.components.TomadyTopBar
import com.tomady.nutrition.ui.rememberTomadyApp
import com.tomady.nutrition.ui.theme.TomadyColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ChatMessage(val from: String, val text: String)

private enum class ModelState { CHECKING, READY, MOCK, DOWNLOADING, UNAVAILABLE }

@Composable
fun AssistantScreen() {
    val app = rememberTomadyApp()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var modelState by remember { mutableStateOf(ModelState.CHECKING) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadPromptDismissed by remember { mutableStateOf(false) }
    var showDownloadConfirm by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf<String?>(null) }

    fun refreshStatus() {
        modelState = when {
            app.gemmaService.isModelLoaded() && !app.gemmaService.isUsingMockFallback() -> ModelState.READY
            app.gemmaService.isModelLoaded() && app.gemmaService.isUsingMockFallback() -> ModelState.MOCK
            else -> ModelState.UNAVAILABLE
        }
    }

    LaunchedEffect(Unit) { refreshStatus() }

    fun startDownload() {
        modelState = ModelState.DOWNLOADING
        scope.launch {
            val progressJob = launch {
                while (true) {
                    app.gemmaService.getDownloadProgress()?.let { downloadProgress = it }
                    delay(400)
                }
            }
            val path = app.gemmaService.downloadModelIfNeeded()
            app.gemmaService.loadModel(path)
            progressJob.cancel()
            refreshStatus()
        }
    }

    suspend fun actuallySend(text: String) {
        sending = true
        messages = messages + ChatMessage("user", text)
        if (!app.gemmaService.isModelLoaded()) {
            app.gemmaService.loadModel()
        }
        val answer = try {
            app.gemmaService.askQuestion(text, CURRENT_USER_ID).answer
        } catch (e: Exception) {
            "Désolé, une erreur est survenue : ${e.message}"
        }
        messages = messages + ChatMessage("ai", answer)
        sending = false
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        input = ""

        if (modelState != ModelState.READY && !downloadPromptDismissed) {
            pendingText = text
            showDownloadConfirm = true
            return
        }
        scope.launch { actuallySend(text) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TomadyTopBar(title = "Assistant Tomady") {
                when (modelState) {
                    ModelState.READY -> AiStatusPillPresets.Ready()
                    ModelState.MOCK -> AiStatusPillPresets.Mock(onDownload = ::startDownload)
                    ModelState.DOWNLOADING -> AiStatusPillPresets.Downloading(downloadProgress)
                    ModelState.UNAVAILABLE -> AiStatusPillPresets.Unavailable(onRetry = ::startDownload)
                    ModelState.CHECKING -> {}
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    SectionCard {
                        Text(
                            if (msg.from == "user") "Vous" else "Tomady",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (msg.from == "user") TomadyColors.ink else TomadyColors.violetDeep
                        )
                        Text(
                            msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TomadyColors.inkSoft,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Écrire un message…") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = ::send, enabled = !sending && input.isNotBlank()) {
                    Icon(Icons.Filled.Send, contentDescription = "Envoyer", tint = TomadyColors.violet)
                }
            }
        }
    }

    if (showDownloadConfirm) {
        AlertDialog(
            onDismissRequest = { showDownloadConfirm = false },
            title = { Text("Modèle IA non téléchargé") },
            text = {
                Text(
                    "L'assistant utilise actuellement des réponses de démonstration. " +
                        "Téléchargez le modèle Gemma réel pour des réponses personnalisées et hors-ligne."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDownloadConfirm = false
                    pendingText = null
                    startDownload()
                }) { Text("Télécharger le modèle") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDownloadConfirm = false
                    downloadPromptDismissed = true
                    val text = pendingText
                    pendingText = null
                    if (text != null) scope.launch { actuallySend(text) }
                }) { Text("Continuer en mode démo") }
            }
        )
    }
}
