#!/bin/bash

echo "🚀 Starting Eden M-Kopa Server..."

# Start the unified server
gunicorn --bind 0.0.0.0:${PORT:-10000} --workers 2 --threads 4 --timeout 120 server:app
