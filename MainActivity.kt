package com.mickai.pet

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class ChatLine(val fromUser: Boolean, val content: String)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent {
   MaterialTheme {
    val scope = rememberCoroutineScope()
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf(ChatLine(false, "G'day! I'm Mick. Configure your secure AI server to start chatting.")) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    Scaffold(topBar = { Text("Mick AI • Personal Companion", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge) }) { insets ->
     Column(Modifier.fillMaxSize().padding(insets).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       Button(onClick = {
        if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (!Settings.canDrawOverlays(this@MainActivity)) {
         startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
         startForegroundService(Intent(this@MainActivity, PetService::class.java))
        }
       }) { Text("Float Mick") }
       OutlinedButton(onClick = { stopService(Intent(this@MainActivity, PetService::class.java)) }) { Text("Stop floating") }
      }
      OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("HTTPS backend URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
      OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Access token (session only)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
      LazyColumn(modifier = Modifier.weight(1f)) {
       items(messages) { line ->
        Text((if (line.fromUser) "You: " else "Mick: ") + line.content, modifier = Modifier.padding(vertical = 8.dp))
       }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Ask Mick") }, modifier = Modifier.weight(1f), maxLines = 3)
       Button(enabled = !busy && message.isNotBlank() && endpoint.startsWith("https://") && token.isNotBlank(), onClick = {
        val prompt = message.trim()
        message = ""
        messages.add(ChatLine(true, prompt))
        busy = true
        scope.launch {
         val response = try { ChatClient(endpoint, token).reply(prompt) } catch (e: Exception) { "Couldn't connect: ${e.message ?: "Unknown error"}" }
         messages.add(ChatLine(false, response))
         busy = false
        }
       }) { Text(if (busy) "..." else "Send") }
      }
     }
    }
   }
  }
 }
}
