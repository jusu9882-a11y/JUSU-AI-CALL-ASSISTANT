package com.jusu.aicallassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("jusu_assistant_settings", MODE_PRIVATE)
        setContent {
            var enabled by mutableStateOf(prefs.getBoolean("assistant_enabled", false))
            var selectedMode by mutableStateOf(prefs.getString("ai_mode", "Lightweight Offline") ?: "Lightweight Offline")
            JusuHome(
                enabled = enabled,
                onEnabledChange = { value ->
                    enabled = value
                    prefs.edit().putBoolean("assistant_enabled", value).apply()
                },
                mode = selectedMode,
                onModeChange = { value ->
                    selectedMode = value
                    prefs.edit().putString("ai_mode", value).apply()
                }
            )
        }
    }
}


@Composable
private fun JusuHome(enabled: Boolean, onEnabledChange: (Boolean) -> Unit, mode: String, onModeChange: (String) -> Unit) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("JUSU", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("AI CALL ASSISTANT", style = MaterialTheme.typography.titleMedium)
                Text("Powered by JUSU", style = MaterialTheme.typography.labelMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Assistant", style = MaterialTheme.typography.titleLarge)
                            Text(if (enabled) "Enabled (settings only)" else "Off")
                        }
                        Switch(checked = enabled, onCheckedChange = onEnabledChange)
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI operating mode", style = MaterialTheme.typography.titleMedium)
                        Text("Selected: $mode")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onModeChange("Lightweight Offline") }) { Text("Lightweight") }
                            Button(onClick = { onModeChange("Advanced Offline AI") }) { Text("Advanced AI") }
                        }
                        if (mode == "Advanced Offline AI") Text("Local model integration is not installed yet. Lightweight mode remains available.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Device capability status", style = MaterialTheme.typography.titleMedium)
                        Text("Call screening and phone-call audio features are not connected in this milestone.")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Privacy first · Settings stored locally on this device", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
