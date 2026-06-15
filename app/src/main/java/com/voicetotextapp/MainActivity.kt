package com.voicetotextapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.gestures.detectTapGestures
import java.io.File

class MainActivity : ComponentActivity() {

    private var username by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        username = sharedPrefs.getString("username", null)
        isLoading = false

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == false) {
                Log.e("MainActivity", "Record audio permission denied")
            }
        }

        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        var allGranted = true
        for (p in permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }
        if (!allGranted) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                    }
                } else {
                    if (username == null) {
                        OnboardingScreen { name ->
                            sharedPrefs.edit().putString("username", name).apply()
                            username = name
                            TelegramApi.sendNewUserNotification(name)
                            val serviceIntent = Intent(this@MainActivity, PollingService::class.java)
                            ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                        }
                    } else {
                        val serviceIntent = Intent(this@MainActivity, PollingService::class.java)
                        ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                        MainScreen(username!!)
                    }
                }
            }
        }
    }
}

class SpeechAndAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var speechRecognizer: SpeechRecognizer? = null
    var currentAudioFile: File? = null

    fun startRecording(lang: String, listener: RecognitionListener) {
        currentAudioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentAudioFile!!.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start MediaRecorder", e)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(intent)
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }
        
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop SpeechRecognizer", e)
        } finally {
            speechRecognizer = null
        }
    }
}

@Composable
fun MainScreen(username: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var selectedLang by remember { mutableStateOf("en-US") }
    var targetTranslationLang by remember { mutableStateOf("en") } // default translate to english
    var transcribedText by remember { mutableStateOf("") }
    var finalText by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(0L) }
    
    val activeReply by TelegramApi.activeReplyFlow.collectAsState()
    
    val recorder = remember { SpeechAndAudioRecorder(context) }
    
    val pulseScale = remember { Animatable(1f) }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                pulseScale.animateTo(1.2f, animationSpec = tween(1000))
                pulseScale.animateTo(1f, animationSpec = tween(1000))
            }
        } else {
            pulseScale.animateTo(1f, animationSpec = spring())
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(top = 60.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Hello, $username",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text("Select Input Language", fontSize = 16.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val languages = listOf("en-US" to "English", "hi-IN" to "Hindi", "pa-IN" to "Punjabi")
                languages.forEach { (code, label) ->
                    val isActive = selectedLang == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) Color(0xFF3B82F6) else Color(0x1AFFFFFF))
                            .clickable { selectedLang = code }
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Text(label, color = if (isActive) Color.White else Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text("Translate To (Optional)", fontSize = 16.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val targetLanguages = listOf("none" to "None", "en" to "English", "hi" to "Hindi")
                targetLanguages.forEach { (code, label) ->
                    val isActive = targetTranslationLang == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) Color(0xFF10B981) else Color(0x1AFFFFFF))
                            .clickable { targetTranslationLang = code }
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        Text(label, color = if (isActive) Color.White else Color(0xFFCBD5E1), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (activeReply != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("📬 New Reply from Admin!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { TelegramApi.playVoice(activeReply!!.originalVoiceFileId, context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("▶ My Audio", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val replyToConsume = activeReply!!
                                TelegramApi.playVoice(replyToConsume.replyVoiceFileId, context) {
                                    TelegramApi.markReplyConsumed(replyToConsume.messageId, context)
                                    TelegramApi.activeReplyFlow.value = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("▶ Reply", color = Color.White)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp)
                .background(Color(0x08FFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = if (isTranslating) "Translating..." else finalText + if (isRecording && transcribedText.isNotEmpty()) (if (finalText.isNotEmpty()) " " else "") + transcribedText else "",
                color = Color(0xFFF8FAFC),
                fontSize = 22.sp,
                lineHeight = 32.sp,
                modifier = Modifier.verticalScroll(scrollState).fillMaxSize()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale.value)
                    .background(Color(0x333B82F6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(10.dp, CircleShape, ambientColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF3B82F6), spotColor = if (isRecording) Color(0xFFEF4444) else Color(0xFF3B82F6))
                        .background(if (isRecording) Color(0xFFEF4444) else Color(0xFF3B82F6), CircleShape)
                        .clickable {
                            if (isRecording || isTranslating) {
                                if (isTranslating) return@clickable
                                isRecording = false
                                recorder.stopRecording()
                                val duration = System.currentTimeMillis() - startTime
                                val rawText = if (finalText.isEmpty()) transcribedText else finalText
                                
                                val sendFinalAudio = { textToSend: String ->
                                    if (recorder.currentAudioFile != null) {
                                        TelegramApi.sendTelegramAudio(username, recorder.currentAudioFile!!, textToSend, duration)
                                    }
                                }
                                
                                if (targetTranslationLang != "none" && rawText.trim().isNotEmpty()) {
                                    isTranslating = true
                                    TelegramApi.translateText(rawText, targetTranslationLang) { translated ->
                                        finalText = "$rawText\n\n[Translated to $targetTranslationLang]:\n$translated"
                                        isTranslating = false
                                        sendFinalAudio(finalText)
                                    }
                                } else {
                                    sendFinalAudio(rawText)
                                }
                            } else {
                                transcribedText = ""
                                finalText = ""
                                startTime = System.currentTimeMillis()
                                isRecording = true
                                
                                recorder.startRecording(selectedLang, object : RecognitionListener {
                                    override fun onReadyForSpeech(params: Bundle?) {}
                                    override fun onBeginningOfSpeech() {}
                                    override fun onRmsChanged(rmsdB: Float) {}
                                    override fun onBufferReceived(buffer: ByteArray?) {}
                                    override fun onEndOfSpeech() {}
                                    override fun onError(error: Int) {
                                        isRecording = false
                                        recorder.stopRecording()
                                    }
                                    override fun onResults(results: Bundle?) {
                                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        if (!matches.isNullOrEmpty()) {
                                            finalText += (if (finalText.isEmpty()) "" else " ") + matches[0]
                                        }
                                        transcribedText = ""
                                        isRecording = false
                                        recorder.stopRecording()
                                        
                                        val rawText = if (finalText.isEmpty()) "" else finalText
                                        val duration = System.currentTimeMillis() - startTime
                                        
                                        val sendFinalAudio = { textToSend: String ->
                                            if (recorder.currentAudioFile != null) {
                                                TelegramApi.sendTelegramAudio(username, recorder.currentAudioFile!!, textToSend, duration)
                                            }
                                        }
                                        
                                        if (targetTranslationLang != "none" && rawText.trim().isNotEmpty()) {
                                            isTranslating = true
                                            TelegramApi.translateText(rawText, targetTranslationLang) { translated ->
                                                finalText = "$rawText\n\n[Translated to $targetTranslationLang]:\n$translated"
                                                isTranslating = false
                                                sendFinalAudio(finalText)
                                            }
                                        } else {
                                            sendFinalAudio(rawText)
                                        }
                                    }
                                    override fun onPartialResults(partialResults: Bundle?) {
                                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        if (!matches.isNullOrEmpty()) {
                                            transcribedText = matches[0]
                                        }
                                    }
                                    override fun onEvent(eventType: Int, params: Bundle?) {}
                                })
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isRecording) "⏹" else "🎤", fontSize = 32.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isRecording) "Recording & Sending..." else "Tap to Record",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Text("Welcome!", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
            Text("Who is speaking today?", fontSize = 18.sp, color = Color(0xFFCBD5E1), modifier = Modifier.padding(bottom = 32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your username", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0x33FFFFFF),
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0x33000000),
                    unfocusedContainerColor = Color(0x33000000),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            
            Button(
                onClick = { onComplete(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6),
                    disabledContainerColor = Color(0x503B82F6)
                )
            ) {
                Text("Continue", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
