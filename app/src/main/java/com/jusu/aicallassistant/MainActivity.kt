package com.jusu.aicallassistant

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jusu.aicallassistant.data.InstructionRepository
import com.jusu.aicallassistant.calls.WhatsAppCallNotificationListener
import com.jusu.aicallassistant.engine.CallAction
import com.jusu.aicallassistant.engine.CallInstruction
import com.jusu.aicallassistant.engine.CallerScope
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {
    private var screeningRoleHeld by mutableStateOf(false)
    private var contactsPermissionGranted by mutableStateOf(false)
    private var whatsappNotificationAccessGranted by mutableStateOf(false)
    private var lastWhatsAppCallLabel by mutableStateOf("")
    private var lastWhatsAppCallApp by mutableStateOf("")
    private var lastWhatsAppCallTime by mutableStateOf(0L)
    private var roleLauncher: androidx.activity.result.ActivityResultLauncher<Intent>? = null
    private var contactsLauncher: androidx.activity.result.ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissionState()
        }
        contactsLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshPermissionState()
        }
        refreshPermissionState()

        val prefs = getSharedPreferences("jusu_assistant_settings", MODE_PRIVATE)
        val repository = InstructionRepository(applicationContext)
        setContent {
            var enabled by remember { mutableStateOf(prefs.getBoolean("assistant_enabled", false)) }
            var selectedMode by remember { mutableStateOf(prefs.getString("ai_mode", "Lightweight Offline") ?: "Lightweight Offline") }
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
                },
                repository = repository,
                screeningRoleHeld = screeningRoleHeld,
                contactsPermissionGranted = contactsPermissionGranted,
                whatsappNotificationAccessGranted = whatsappNotificationAccessGranted,
                lastWhatsAppCallLabel = lastWhatsAppCallLabel,
                lastWhatsAppCallApp = lastWhatsAppCallApp,
                lastWhatsAppCallTime = lastWhatsAppCallTime,
                onRequestWhatsAppAccess = { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                onRequestScreeningRole = { requestScreeningRole() },
                onRequestContactsPermission = {
                    contactsLauncher?.launch(Manifest.permission.READ_CONTACTS)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        screeningRoleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else false
        contactsPermissionGranted =
            checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val manager = getSystemService(RoleManager::class.java) ?: return
        if (manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            roleLauncher?.launch(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }
}

@Composable
private fun JusuHome(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    mode: String,
    onModeChange: (String) -> Unit,
    repository: InstructionRepository,
    screeningRoleHeld: Boolean,
    contactsPermissionGranted: Boolean,
    onRequestScreeningRole: () -> Unit,
    onRequestContactsPermission: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var instructions by remember { mutableStateOf(emptyList<CallInstruction>()) }
    var activeProfile by remember { mutableStateOf(repository.activeProfile()) }
    var editing by remember { mutableStateOf<CallInstruction?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        repository.instructions.collect { instructions = it }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("JUSU", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("AI CALL ASSISTANT", style = MaterialTheme.typography.titleMedium)
                Text("Powered by JUSU", style = MaterialTheme.typography.labelMedium)

                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Assistant", style = MaterialTheme.typography.titleLarge)
                            Text(if (enabled) "Enabled in app settings" else "Off")
                        }
                        Switch(checked = enabled, onCheckedChange = onEnabledChange)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Call screening permissions", style = MaterialTheme.typography.titleLarge)
                        Text(if (screeningRoleHeld) "Call screening role granted" else "Android call-screening role is not granted.")
                        if (!screeningRoleHeld) {
                            Button(onClick = onRequestScreeningRole, modifier = Modifier.fillMaxWidth()) {
                                Text("Grant call-screening role")
                            }
                        }
                        Text(if (contactsPermissionGranted) "Contacts permission granted" else "Contacts permission not granted. Contact-specific rules are safely skipped until permission is granted.")
                        if (!contactsPermissionGranted) {
                            OutlinedButton(onClick = onRequestContactsPermission, modifier = Modifier.fillMaxWidth()) {
                                Text("Allow contacts access")
                            }
                        }
                        Text("Android displays its own permission/role confirmation. You can decline either request; call screening then remains unavailable or contact matching is limited.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("WhatsApp call support", style = MaterialTheme.typography.titleLarge)
                        Text(if (whatsappNotificationAccessGranted) "WhatsApp call notification access is enabled." else "Enable notification access to let JUSU detect incoming WhatsApp and WhatsApp Business call notifications.")
                        if (!whatsappNotificationAccessGranted) {
                            OutlinedButton(onClick = onRequestWhatsAppAccess, modifier = Modifier.fillMaxWidth()) {
                                Text("Enable WhatsApp call detection")
                            }
                        }
                        if (lastWhatsAppCallTime > 0L) {
                            Text("Last detected: $lastWhatsAppCallApp · $lastWhatsAppCallLabel")
                            Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastWhatsAppCallTime)), style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("No WhatsApp call notifications detected yet.", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Android will ask you to grant notification access in Settings. JUSU filters for WhatsApp call notifications and stores only a short caller label and time locally. Detection does not yet answer, reject, silence, or access WhatsApp call audio.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI operating mode", style = MaterialTheme.typography.titleMedium)
                        Text("Selected: $mode")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onModeChange("Lightweight Offline") }) { Text("Lightweight") }
                            Button(onClick = { onModeChange("Advanced Offline AI") }) { Text("Advanced AI") }
                        }
                        if (mode == "Advanced Offline AI") Text("Local model integration is not installed yet. Lightweight mode remains available.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Instruction control center", style = MaterialTheme.typography.titleLarge)
                        Text("Rules are saved locally on this device and evaluated by the offline decision engine.")
                        Text("Active profile: $activeProfile", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf("Personal", "Professional", "Privacy", "Busy", "Custom").forEach { profile ->
                                TextButton(onClick = {
                                    activeProfile = profile
                                    repository.setActiveProfile(profile)
                                }) { Text(if (activeProfile == profile) "[$profile]" else profile) }
                            }
                        }
                        Button(onClick = { editing = null; showEditor = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("+ Add instruction")
                        }
                        val visibleRules = instructions.filter { it.profile == activeProfile }
                        if (visibleRules.isEmpty()) {
                            Text("No instructions in this profile yet. Add a rule to define how matching callers should be handled.")
                        } else {
                            visibleRules.forEach { rule ->
                                InstructionRow(
                                    rule,
                                    { checked -> scope.launch { repository.save(rule.copy(enabled = checked)) } },
                                    { editing = rule; showEditor = true },
                                    { scope.launch { repository.delete(rule.id) } }
                                )
                            }
                        }
                        Text("${visibleRules.size} instruction(s) · ${visibleRules.count { it.enabled }} active", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Device capability status", style = MaterialTheme.typography.titleMedium)
                        Text(if (screeningRoleHeld) "Android has granted the screening role. Incoming-call integration is registered but still requires real-device validation." else "The offline rule engine is implemented. Grant the Android screening role to let the system bind the call-screening service.")
                        Text("This service does not answer calls, access call audio, or record conversations. SCREEN currently silences ringing; it is not a live AI conversation.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Privacy first · Instructions and settings stay on this device", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showEditor) {
        InstructionEditor(editing, activeProfile, { showEditor = false }, { rule ->
            scope.launch { repository.save(rule) }
            showEditor = false
        })
    }
}

@Composable
private fun InstructionRow(
    rule: CallInstruction,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(rule.title, style = MaterialTheme.typography.titleMedium)
                    Text("${rule.action} · ${rule.scope} · Priority ${rule.priority}", style = MaterialTheme.typography.bodySmall)
                    if (rule.scope == CallerScope.PHONE_NUMBER) Text(rule.phoneNumber.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun InstructionEditor(
    initial: CallInstruction?,
    profile: String,
    onDismiss: () -> Unit,
    onSave: (CallInstruction) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var scopeChoice by remember(initial) { mutableStateOf(initial?.scope ?: CallerScope.ALL) }
    var actionChoice by remember(initial) { mutableStateOf(initial?.action ?: CallAction.ALLOW) }
    var phone by remember(initial) { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var priority by remember(initial) { mutableStateOf(initial?.priority?.toString() ?: "0") }
    var active by remember(initial) { mutableStateOf(initial?.enabled ?: true) }
    val scopes = CallerScope.values().toList()
    val actions = CallAction.values().toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New instruction" else "Edit instruction") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Instruction name") }, singleLine = true)
                Text("Caller scope: $scopeChoice")
                TextButton(onClick = { scopeChoice = scopes[(scopes.indexOf(scopeChoice) + 1) % scopes.size] }) {
                    Text("Change caller scope")
                }
                if (scopeChoice == CallerScope.PHONE_NUMBER) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone number") }, singleLine = true)
                }
                Text("Action: $actionChoice")
                TextButton(onClick = { actionChoice = actions[(actions.indexOf(actionChoice) + 1) % actions.size] }) {
                    Text("Change action")
                }
                OutlinedTextField(value = priority, onValueChange = { priority = it.filter { c -> c.isDigit() || c == '-' }.take(6) }, label = { Text("Priority (higher wins)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Instruction active")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                Text("Profile: $profile", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && (scopeChoice != CallerScope.PHONE_NUMBER || phone.isNotBlank()),
                onClick = {
                    onSave(CallInstruction(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = title.trim(),
                        scope = scopeChoice,
                        action = actionChoice,
                        phoneNumber = if (scopeChoice == CallerScope.PHONE_NUMBER) phone.trim() else null,
                        enabled = active,
                        priority = priority.toIntOrNull() ?: 0,
                        profile = profile
                    ))
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
