package com.assistant.infera

import com.intellij.openapi.application.ApplicationManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

object SuggestionService {
    fun fetchStreamingSuggestion(
        code: String,
        line: Int,
        language: String,
        onToken: (String) -> Unit,
        onDone: () -> Unit
    ) {
        val prompt = "Continue this $language code:\n$code"

        val json = JSONObject()
            .put("model", "tinyllama") // Or "llama3", or any model your Ollama server supports
            .put("prompt", prompt)
            .put("stream", true)

        val requestBody =
            RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("http://localhost:11434/api/generate")
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                ApplicationManager.getApplication().invokeLater {
                    onToken("Error: ${e.message}")
                    onDone()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.source()?.let { source ->
                    try {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: continue
                                val jsonText = line.trim()
                                if (jsonText.isNotEmpty()) {
                                    val token = JSONObject(jsonText).optString("response", "")
                                    ApplicationManager.getApplication().invokeLater {
                                        onToken(token)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        ApplicationManager.getApplication().invokeLater {
                            onToken("Stream Error: ${e.message}")
                        }
                    } finally {
                        ApplicationManager.getApplication().invokeLater {
                            onDone()
                        }
                    }
                } ?: ApplicationManager.getApplication().invokeLater {
                    onToken("Empty response body")
                    onDone()
                }
            }
        })
    }
}