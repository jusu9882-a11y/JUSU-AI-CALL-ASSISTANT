package com.jusu.aicallassistant.intelligence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun JusuChatScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ChatHistoryStore(context.applicationContext) }
    val brain = remember { ExpertBrain() }
    var turns by remember { mutableStateOf(store.load()) }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun sendMessage(raw: String) {
        val message = raw.trim()
        if (message.isEmpty()) return
        val userTurn = ChatTurn("user", message)
        val reply = brain.respond(message, turns)
        val assistantTurn = ChatTurn("assistant", reply.text)
        store.append(userTurn, assistantTurn)
        turns = (turns + userTurn + assistantTurn).takeLast(80)
        input = ""
        scope.launch { if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex) }
    }

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.lastIndex)
    }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("JUSU", style = MaterialTheme.typography.headlineSmall)
                Text("Private conversation · Stored on this device", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClose) { Text("Close") }
        }
        Text("OFFLINE · LOCAL RULES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            if (turns.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Hello. I'm JUSU.", style = MaterialTheme.typography.titleLarge)
                            Text("We can talk about the call assistant. This conversation stays on your phone. My current answers use local rules; a full offline language model is not connected yet.")
                            Text("Try one of these:", style = MaterialTheme.typography.labelLarge)
                            listOf("What can you do?", "How does call protection work?", "How does WhatsApp support work?", "What's next?").forEach { suggestion ->
                                OutlinedButton(onClick = { sendMessage(suggestion) }, modifier = Modifier.fillMaxWidth()) { Text(suggestion) }
                            }
                        }
                    }
                }
            }
            items(turns) { turn ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "user") Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 320.dp), horizontalAlignment = if (turn.role == "user") Alignment.End else Alignment.Start) {
                        Text(if (turn.role == "user") "You" else "JUSU · Local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(color = if (turn.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                            Text(turn.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message JUSU…") }, maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage(input) }))
            Button(enabled = input.isNotBlank(), onClick = { sendMessage(input) }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) { Text("Send") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("No cloud connection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { store.clear(); turns = emptyList(); input = "" }) { Text("Clear chat") }
        }
    }
}
