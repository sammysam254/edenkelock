FROM python:3.11-slim

WORKDIR /app

# Copy requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY server.py .
COPY templates/ ./templates/
COPY static/ ./static/
COPY app/ ./app/

# Expose port
EXPOSE 10000

# Run server
CMD ["gunicorn", "--bind", "0.0.0.0:10000", "--workers", "2", "--threads", "4", "server:app"]
