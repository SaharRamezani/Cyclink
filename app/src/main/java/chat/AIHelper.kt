package com.example.cyclink.helpers

import android.util.Log
import com.example.cyclink.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.content.Context
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AIHelper(private val context: Context) {
    private val apiKey = BuildConfig.AI_STUDIO_API_KEY.takeIf { it.isNotEmpty() }
        ?: "***REMOVED***"

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun sendMessage(message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("AIHelper", "API Key length: ${apiKey.length}")
            Log.d("AIHelper", "API Key starts with: ${apiKey.take(10)}...")

            if (apiKey.isEmpty() || apiKey == "null" || apiKey == "\"\"") {
                return@withContext Result.failure(Exception("API key not configured"))
            }

            // Add network connectivity check
            if (!isNetworkAvailable()) {
                return@withContext Result.failure(Exception("No network connection available"))
            }

            val url = URL("$baseUrl?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection

            // Add timeout settings for better error handling
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 30000 // 30 seconds
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = AIRequest(
                contents = listOf(
                    RequestContent(
                        parts = listOf(RequestPart(text = message))
                    )
                )
            )

            val jsonRequest = json.encodeToString(requestBody)
            Log.d("AIHelper", "Request: $jsonRequest")

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonRequest)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d("AIHelper", "Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                Log.d("AIHelper", "Response: $response")

                val aiResponse = json.decodeFromString<AIResponse>(response)
                val aiText = aiResponse.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Sorry, I couldn't generate a response."

                Result.success(aiText)
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e("AIHelper", "Error response: $errorResponse")
                Result.failure(Exception("API request failed with code $responseCode: $errorResponse"))
            }

        } catch (e: java.net.UnknownHostException) {
            Log.e("AIHelper", "Network error - unable to resolve host: ${e.message}")
            Result.failure(Exception("Network connection error. Please check your internet connection."))
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AIHelper", "Request timeout: ${e.message}")
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: java.io.IOException) {
            Log.e("AIHelper", "Network I/O error: ${e.message}")
            Result.failure(Exception("Network error. Please check your connection."))
        } catch (e: Exception) {
            Log.e("AIHelper", "Error sending message", e)
            Result.failure(e)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities != null && (
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                    )
        } catch (e: Exception) {
            false
        }
    }
}