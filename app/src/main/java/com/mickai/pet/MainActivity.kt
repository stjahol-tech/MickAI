package com.mickai.pet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private data class ChatLine(val fromUser: Boolean, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val scope = rememberCoroutineScope()
                var endpoint by remember { mutableStateOf("") }
                var token by remember { mutableStateOf("") }
                var message by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                val messages = remember {
                    mutableStateListOf(ChatLine(false, "G'day! I'm Mick. Configure your secure AI server to start chatting."))
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Mick AI • Personal Companion") }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { /* overlay UI starts in the service */ }) {
                                Text("Float Mick")
                            }
                            OutlinedButton(onClick = { /* service stop handled by intent */ }) {
                                Text("Stop floating")
                            }
                        }

                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("HTTPS backend URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )

                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Access token (session only)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { line ->
                                Surface(
                                    color = if (line.fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${if (line.fromUser) "You" else "Mick"}: ${line.content}",
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                label = { Text("Ask Mick") },
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )

                            Button(
                                enabled = !busy && message.isNotBlank() && endpoint.startsWith("https://") && token.isNotBlank(),
                                onClick = {
                                    val prompt = message.trim()
                                    message = ""
                                    messages += ChatLine(true, prompt)
                                    busy = true
                                    scope.launch {
                                        val response = try {
                                            ChatClient(endpoint, token).reply(prompt)
                                        } catch (e: Exception) {
                                            "Couldn't connect: ${e.message ?: "Unknown error"}"
                                        }
                                        messages += ChatLine(false, response)
                                        busy = false
                                    }
                                }
                            ) {
                                Text(if (busy) "..." else "Send")
                            }
                        }
                    }
                }
            }
        }
    }
}
