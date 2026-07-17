FROM python:3.12-slim

WORKDIR /app

# Build deps needed by some pinned packages (cryptography, tokenizers, lancedb, etc.)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

COPY . .

# Added -m flag to create home directory for CrewAI local cache configurations
RUN groupadd -r tracepilot && useradd -m -r -g tracepilot tracepilot \
    && chown -R tracepilot:tracepilot /app
USER tracepilot

EXPOSE 8001

ENV PYTHONUNBUFFERED=1

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
    CMD curl -f http://localhost:8001/ai-worker/v1/health || exit 1

CMD ["python", "-m", "app.main"]