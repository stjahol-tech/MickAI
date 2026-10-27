package com.mickai.pet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatClient(private val endpoint: String, private val accessToken: String) {
 init {
  require(endpoint.startsWith("https://")) { "A secure HTTPS endpoint is required" }
 }
 suspend fun reply(message: String): String = withContext(Dispatchers.IO) {
  val connection = (URL(endpoint.trimEnd('/') + "/chat").openConnection() as HttpURLConnection)
  try {
   connection.requestMethod = "POST"
   connection.connectTimeout = 12000
   connection.readTimeout = 60000
   connection.setRequestProperty("Content-Type", "application/json")
   connection.setRequestProperty("Authorization", "Bearer $accessToken")
   connection.doOutput = true
   val body = JSONObject().put("message", message).toString()
   connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
   if (connection.responseCode !in 200..299) throw IllegalStateException("Server returned HTTP ${connection.responseCode}")
   val result = connection.inputStream.bufferedReader().use { it.readText() }
   JSONObject(result).getString("reply")
  } finally { connection.disconnect() }
 }
}
