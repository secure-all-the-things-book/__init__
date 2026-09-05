#!/usr/bin/env bash

docker run   -p 8000:8000 \
  -v ~/.cache/huggingface:/root/.cache/huggingface \
  fedirz/faster-whisper-server:latest-cpu 