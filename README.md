# LinguaCraft Live 🎙️🌍
Real-time voice transcription & translation for Minecraft  
**Fabric 1.21.11 + Simple Voice Chat**

LinguaCraft Live adds QSMP-style multilingual communication to your server:
players speak in voice chat and nearby players see **live translated chat bubbles** above the speaker — in *their* chosen language.

---

## ✨ Features
- 🎤 Voice → text transcription (Whisper / faster-whisper backend)
- ⌨️ **Partial subtitles while speaking** (typing effect)
- 🌍 **Per-viewer language setting**
- 🔊 **True proximity & group routing** based on Simple Voice Chat audibility
- 👀 Bubbles show **only when you look at the speaker**
- ⚙️ Configurable backend URL, timing, ranges, UI

---

## 📦 Repo layout
- `linguacraft_live/` — Fabric mod (client + server)
- `backend/` — WebSocket backend (STT + translation) + Docker

---

## ✅ Requirements
### Minecraft
- Minecraft **1.21.11**
- Fabric Loader
- Fabric API
- Simple Voice Chat (client + server)

### Java
- **Java 21**

### Backend
- Docker (recommended) or Python 3.12+

---

## 🚀 Quick start

### 1) Start backend
```bash
cd backend
docker compose up --build
```
Backend URL:
`ws://127.0.0.1:8010/ws`

### 2) Build mod jar
```bash
cd linguacraft_live
./gradlew build
```
Jar output:
`linguacraft_live/build/libs/`

### 3) Install mods
Server + clients:
- Fabric API
- Simple Voice Chat
- LinguaCraft Live jar

---

## 🌍 Commands
Set your preferred translation language:
```text
/lingua en
/lingua fr
/lingua de
```
This is saved client-side and synced to the server.

---

## ⚙️ Configuration
Server + client shared:
`config/linguacraft_live.json`

Client-only:
`config/linguacraft_live_client.json`

---

## 🖥️ Backend recommended specs
### Small SMP (10–20 players)
- 4 vCPU, 8 GB RAM, Whisper `small`, no GPU

### Medium (20–50 players)
- 8 vCPU, 16 GB RAM, Whisper `small/medium`

### Production (50–150 players)
- 8–12 vCPU, 32 GB RAM, **NVIDIA T4/RTX 3060**, Whisper `medium/large-v3`

---

## 🔐 Privacy notice
Voice is processed server-side for transcription. Inform players and obtain consent.

---

## 📜 License
MIT (recommended)
