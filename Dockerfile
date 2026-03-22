FROM node:18-alpine AS builder

# Build dashboard
WORKDIR /app/dashboard
COPY dashboard/package*.json ./
RUN npm install
COPY dashboard/ ./
RUN npm run build

# Python runtime
FROM python:3.11-slim

WORKDIR /app

# Copy Python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy server
COPY server.py .

# Copy built dashboard
COPY --from=builder /app/dashboard/out ./dashboard/out

# Expose port
EXPOSE 10000

# Run server
CMD ["gunicorn", "--bind", "0.0.0.0:10000", "--workers", "2", "--threads", "4", "server:app"]
