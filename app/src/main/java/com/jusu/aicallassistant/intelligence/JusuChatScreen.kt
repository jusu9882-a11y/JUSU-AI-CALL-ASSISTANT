package com.jusu.aicallassistant.intelligence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("JUSU Chat", style = MaterialTheme.typography.headlineSmall)
                Text("Local chat foundation · No cloud connection", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onClose) { Text("Close") }
        }
        Card(Modifier.fillMaxWidth()) {
            Text("I can answer basic app questions locally. Full expert language-model reasoning and book-based research are not connected yet.", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (turns.isEmpty()) {
                item {
                    Text("Hello! I'm JUSU. Ask me about the call assistant or how its safety rules work.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(turns) { turn ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (turn.role == "user") Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (turn.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(turn.text, Modifier.padding(12.dp))
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message JUSU…") },
                maxLines = 4
            )
            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    val message = input.trim()
                    val userTurn = ChatTurn("user", message)
                    val reply = brain.respond(message, turns)
                    val assistantTurn = ChatTurn("assistant", reply.text)
                    store.append(userTurn, assistantTurn)
                    turns = (turns + userTurn + assistantTurn).takeLast(80)
                    input = ""
                    scope.launch { listState.animateScrollToItem((turns.size - 1).coerceAtLeast(0)) }
                }
            ) { Text("Send") }
        }
        TextButton(onClick = {
            store.clear()
            turns = emptyList()
        }, modifier = Modifier.align(Alignment.End)) { Text("Clear local chat history") }
    }
}
