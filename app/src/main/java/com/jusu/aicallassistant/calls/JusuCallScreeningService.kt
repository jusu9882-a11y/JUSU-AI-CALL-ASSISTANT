package com.jusu.aicallassistant.calls

import android.Manifest
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import com.jusu.aicallassistant.data.InstructionRepository
import com.jusu.aicallassistant.engine.CallAction
import com.jusu.aicallassistant.engine.CallDecisionEngine
import com.jusu.aicallassistant.engine.CallerInfo
import com.jusu.aicallassistant.engine.CallerScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android's user-approved call-screening role is required for this service to receive calls.
 * No call audio is captured or recorded here.
 */
class JusuCallScreeningService : CallScreeningService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine = CallDecisionEngine()

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        serviceScope.launch {
            val number = callDetails.handle?.schemeSpecificPart
            try {
                val repository = InstructionRepository(applicationContext)
                val hasContactsPermission =
                    checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                val isContact = hasContactsPermission && number != null && isSavedContact(number)
                val rules = repository.all().let { all ->
                    if (hasContactsPermission) all
                    else all.filter { it.scope != CallerScope.CONTACTS && it.scope != CallerScope.UNKNOWN }
                }
                val decision = engine.decide(
                    CallerInfo(phoneNumber = number, isContact = isContact),
                    rules,
                    repository.activeProfile()
                )
                val response = when (decision.action) {
                    CallAction.ALLOW -> CallResponse.Builder().build()
                    CallAction.DECLINE -> CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .build()
                    CallAction.SILENCE, CallAction.SCREEN -> CallResponse.Builder()
                        .setSilenceCall(true)
                        .build()
                }
                respondToCall(callDetails, response)
            } catch (_: Exception) {
                // Fail open: a database or runtime error must not accidentally block a caller.
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun isSavedContact(number: String): Boolean {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )?.use { it.moveToFirst() } ?: false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
