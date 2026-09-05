#!/usr/bin/env bash

curl http://localhost:8000/v1/audio/transcriptions -F "file=@./pt2.mp4" -F "model=Systran/faster-whisper-small" -o pt2.json



