# LinguaCraft Live (Fabric 1.21.11)

This bundle contains:
- `linguacraft_live/` -> Fabric mod (server + client) that hooks Simple Voice Chat, transcribes speech, translates it per-viewer, and shows a look-at bubble.
- `backend/` -> Python WebSocket backend (STT + translation) + Docker Compose.

## What it does
- Server listens to Simple Voice Chat **microphone packets** (Opus) and decodes them.
- It sends audio chunks to the backend.
- Backend returns `transcript.partial` and `transcript.final` with translations.
- Server routes messages to the players who SVC is actually sending audio to (group/proximity), by tracking SVC sound packet events.
- Client shows a bubble only when you **look at** the speaking player.

## Requirements
- Minecraft 1.21.11
- Fabric Loader
- Fabric API **0.139.4+1.21.11**
- Simple Voice Chat **fabric-1.21.11-2.6.10** (client + server)
- Java 21

## Build the mod
From `linguacraft_live/`:
```bash
./gradlew build
```

The jar will be in:
`linguacraft_live/build/libs/`

### Note about yarn mappings
`gradle.properties` uses `yarn_mappings=1.21.11+build.1`.
If that exact build isn't available in your environment, change it to the available build for 1.21.11.

## Run backend (recommended: Docker)
From `backend/`:
```bash
docker compose up --build
```

Backend will listen on `ws://127.0.0.1:8010/ws`.

## Config
- Server+client shared config: `config/linguacraft_live.json`
- Client preference: `config/linguacraft_live_client.json`

Client command:
- `/lingua <en|fr|de|...>` sets your preferred translation language.

## Privacy / Consent
This captures voice packets on the server for transcription. Inform players in your server rules/MOTD.
