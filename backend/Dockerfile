FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 8010
ENV DEVICE=cpu
ENV WHISPER_MODEL=small
ENV SRC_LANG_DEFAULT=es

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8010"]
