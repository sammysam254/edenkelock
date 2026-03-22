# Eden M-Kopa Backend

Python Web3 blockchain listener for automatic payment detection.

## Features

- Monitors blockchain for token transfers
- Automatically creates payment records in Supabase
- Triggers device unlock when payment received
- Configurable polling interval
- Error handling and logging

## Tech Stack

- Python 3.11
- Web3.py (Blockchain interaction)
- Supabase Python Client
- Flask (optional REST endpoints)

## Getting Started

1. Create virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

3. Set up environment variables:
```bash
cp .env.example .env
```

Edit `.env` with your credentials.

4. Run the listener:
```bash
python main.py
```

## Environment Variables

Required:
- `SUPABASE_URL` - Your Supabase project URL
- `SUPABASE_SERVICE_KEY` - Your Supabase service role key
- `RPC_URL` - Web3 RPC endpoint (e.g., Infura)
- `CONTRACT_ADDRESS` - Token contract address
- `POLL_INTERVAL` - Polling interval in seconds (default: 30)

## How It Works

1. Listener polls blockchain every 30 seconds
2. Checks for Transfer events to device wallet addresses
3. Queries Supabase for device by wallet address
4. Creates payment transaction record
5. Database trigger updates device balance and lock status
6. Device syncs and unlocks automatically

## Deployment

### Render

1. Create new Web Service
2. Connect GitHub repo
3. Set build command: `pip install -r requirements.txt`
4. Set start command: `python main.py`
5. Add environment variables
6. Deploy

### Docker

```bash
docker build -t eden-backend .
docker run -d --env-file .env eden-backend
```

## Monitoring

Check logs for:
- Payment processing confirmations
- Blockchain connection status
- Error messages

## Troubleshooting

### Not detecting payments
- Check RPC_URL is correct and accessible
- Verify CONTRACT_ADDRESS is correct
- Check Supabase credentials
- Review logs for errors

### Database connection errors
- Verify SUPABASE_SERVICE_KEY is correct
- Check network connectivity
- Ensure RLS policies allow service role access

## License

MIT
