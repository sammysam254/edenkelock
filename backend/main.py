import os
import time
from web3 import Web3
from supabase import create_client, Client
from dotenv import load_dotenv
from datetime import datetime
import logging

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configuration
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY")
RPC_URL = os.getenv("RPC_URL")
CONTRACT_ADDRESS = os.getenv("CONTRACT_ADDRESS")
POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "30"))

# Initialize clients
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)
w3 = Web3(Web3.HTTPProvider(RPC_URL))

# Simple ERC20 Transfer event ABI
TRANSFER_EVENT_ABI = {
    "anonymous": False,
    "inputs": [
        {"indexed": True, "name": "from", "type": "address"},
        {"indexed": True, "name": "to", "type": "address"},
        {"indexed": False, "name": "value", "type": "uint256"}
    ],
    "name": "Transfer",
    "type": "event"
}

def get_last_processed_block():
    """Get the last processed block from a simple file"""
    try:
        with open("last_block.txt", "r") as f:
            return int(f.read().strip())
    except FileNotFoundError:
        return w3.eth.block_number - 100

def save_last_processed_block(block_number):
    """Save the last processed block"""
    with open("last_block.txt", "w") as f:
        f.write(str(block_number))

def process_payment(tx_hash, from_address, to_address, amount_wei):
    """Process a payment transaction"""
    try:
        # Convert amount from wei to token units
        amount = float(w3.from_wei(amount_wei, 'ether'))
        
        # Find device by wallet address
        device_response = supabase.table("devices").select("*").eq("wallet_address", to_address.lower()).execute()
        
        if not device_response.data:
            logger.warning(f"No device found for wallet {to_address}")
            return
        
        device = device_response.data[0]
        
        # Create payment transaction
        payment_data = {
            "device_id": device["id"],
            "customer_id": device["customer_id"],
            "amount": amount,
            "payment_method": "crypto",
            "wallet_transaction_hash": tx_hash,
            "status": "completed"
        }
        
        supabase.table("payment_transactions").insert(payment_data).execute()
        logger.info(f"Payment processed: {amount} tokens for device {device['device_code']}")
        
    except Exception as e:
        logger.error(f"Error processing payment: {e}")

def listen_for_payments():
    """Main listener loop"""
    logger.info("Starting Web3 payment listener...")
    
    last_block = get_last_processed_block()
    
    while True:
        try:
            current_block = w3.eth.block_number
            
            if current_block > last_block:
                logger.info(f"Scanning blocks {last_block + 1} to {current_block}")
                
                # Get transfer events
                event_filter = w3.eth.filter({
                    "fromBlock": last_block + 1,
                    "toBlock": current_block,
                    "address": CONTRACT_ADDRESS
                })
                
                events = event_filter.get_all_entries()
                
                for event in events:
                    if event['topics'][0].hex() == w3.keccak(text="Transfer(address,address,uint256)").hex():
                        from_addr = "0x" + event['topics'][1].hex()[-40:]
                        to_addr = "0x" + event['topics'][2].hex()[-40:]
                        amount = int(event['data'].hex(), 16)
                        
                        process_payment(
                            event['transactionHash'].hex(),
                            from_addr,
                            to_addr,
                            amount
                        )
                
                last_block = current_block
                save_last_processed_block(last_block)
            
            time.sleep(POLL_INTERVAL)
            
        except Exception as e:
            logger.error(f"Error in listener loop: {e}")
            time.sleep(POLL_INTERVAL)

if __name__ == "__main__":
    listen_for_payments()
