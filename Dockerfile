FROM node:18-alpine AS builder

# Build dashboard
WORKDIR /app/dashboard
COPY dashboard/package*.json ./
RUN npm install
COPY dashboard/ ./

# Set build-time environment variables
ENV NEXT_PUBLIC_SUPABASE_URL=https://fvkjeteywfcppbtovbiv.supabase.co
ENV NEXT_PUBLIC_SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ2a2pldGV5d2ZjcHBidG92Yml2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxOTU2NzEsImV4cCI6MjA4OTc3MTY3MX0.5pOcpCSWn98Vvmq4IBQkWWv-nvvA6zbeUZXjSQ3cfC0

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
COPY --from=builder /app/dashboard/.next ./dashboard/.next
COPY --from=builder /app/dashboard/package.json ./dashboard/package.json
COPY --from=builder /app/dashboard/node_modules ./dashboard/node_modules

# Expose port
EXPOSE 10000

# Run server
CMD ["gunicorn", "--bind", "0.0.0.0:10000", "--workers", "2", "--threads", "4", "server:app"]
