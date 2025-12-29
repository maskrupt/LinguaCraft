# LinguaCraft Live Backend

WebSocket endpoint:
- ws://127.0.0.1:8010/ws

## Run with Docker
```bash
docker compose up --build
```

## Run locally
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8010
```

### GPU
Set:
- DEVICE=cuda
- WHISPER_MODEL=medium (or larger)
