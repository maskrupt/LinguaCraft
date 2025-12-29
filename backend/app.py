import base64
import json
import os
import numpy as np
from fastapi import FastAPI, WebSocket
from faster_whisper import WhisperModel
from transformers import pipeline

app = FastAPI()

DEVICE = os.getenv("DEVICE", "cpu")
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "small")
SRC_LANG_DEFAULT = os.getenv("SRC_LANG_DEFAULT", "es")

# You can run faster with GPU by setting DEVICE=cuda (and using an appropriate image/torch build)
stt = WhisperModel(WHISPER_MODEL, device=DEVICE, compute_type="int8" if DEVICE == "cpu" else "float16")

# Translation pipelines cache by model name
_translators = {}

def get_translator(src: str, tgt: str):
    key = f"{src}->{tgt}"
    if key in _translators:
        return _translators[key]
    # Default: Helsinki opus-mt
    model = os.getenv(f"MT_MODEL_{src.upper()}_{tgt.upper()}", f"Helsinki-NLP/opus-mt-{src}-{tgt}")
    _translators[key] = pipeline("translation", model=model)
    return _translators[key]

@app.websocket("/ws")
async def ws_endpoint(ws: WebSocket):
    await ws.accept()
    while True:
        msg = await ws.receive_text()
        data = json.loads(msg)

        if data.get("type") != "audio.chunk":
            continue

        speaker = data["speakerUuid"]
        src_lang = data.get("srcLang", SRC_LANG_DEFAULT)
        is_final = bool(data.get("isFinal", False))
        sr = int(data["sampleRate"])
        targets = data.get("targets", ["en"])

        raw = base64.b64decode(data["pcm16leB64"])
        audio = np.frombuffer(raw, dtype=np.int16).astype(np.float32) / 32768.0

        # Interim vs final:
        # - For partial chunks, transcribe but do less work (faster settings)
        # - For final chunks, allow VAD and better quality
        vad_filter = True if is_final else False

        segments, info = stt.transcribe(
            audio,
            language=src_lang if src_lang else None,
            vad_filter=vad_filter,
            sample_rate=sr
        )

        text_src = " ".join([seg.text.strip() for seg in segments]).strip()
        if not text_src:
            continue

        translations = {}
        for tgt in targets:
            tgt = (tgt or "").strip().lower()
            if not tgt:
                continue
            if tgt == src_lang:
                translations[tgt] = text_src
                continue
            tr = get_translator(src_lang, tgt)
            try:
                translations[tgt] = tr(text_src, max_length=256)[0]["translation_text"]
            except Exception:
                pass

        out = {
            "type": "transcript.final" if is_final else "transcript.partial",
            "speakerUuid": speaker,
            "srcLang": src_lang,
            "textSrc": text_src,
            "translations": translations
        }
        await ws.send_text(json.dumps(out))
