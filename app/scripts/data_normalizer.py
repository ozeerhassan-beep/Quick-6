import re
import pandas as pd
from rapidfuzz import process, fuzz

# Sample raw brochure data extracted from different supermarkets
raw_data = [
    {"store": "DreamPrice", "raw": "Pepsi 2lts", "price": 90.95},
    {"store": "SuperU", "raw": "Pepsi / Mirinda / 7up la bouteille de 1.5L", "price": 82.00},
    {"store": "WAY", "raw": "Pepsi Regular 2 Litres", "price": 99.80}
]

# Master catalog of canonical product names
master_catalog = [
    "Pepsi Regular",
    "Mirinda Orange",
    "7 Up"
]

def parse_and_normalize(item_text):
    text_lower = item_text.lower()
    
    # 1. Extract volume using regex
    vol_match = re.search(r'(\d+(?:\.\d+)?)\s*(?:lts|lt|l|litres|liter)', text_lower)
    volume_ml = float(vol_match.group(1)) * 1000 if vol_match else 1000.0
    
    # 2. Clean item name for fuzzy matching
    clean_name = re.sub(r'(\d+(?:\.\d+)?)\s*(?:lts|lt|l|litres|liter)', '', text_lower)
    clean_name = clean_name.split('/')[0].replace('la bouteille de', '').strip().title()
    
    # 3. Match against Master Catalog using RapidFuzz
    match_result = process.extractOne(clean_name, master_catalog, scorer=fuzz.token_sort_ratio)
    canonical_name = match_result[0] if match_result and match_result[1] > 50 else clean_name
    
    return canonical_name, volume_ml

# Process items
processed_records = []
for entry in raw_data:
    canonical_name, volume_ml = parse_and_normalize(entry["raw"])
    unit_price = entry["price"] / (volume_ml / 1000) # Price per Liter
    
    processed_records.append({
        "Store": entry["store"],
        "Raw Text": entry["raw"],
        "Canonical Product": canonical_name,
        "Volume (ml)": volume_ml,
        "Price (Rs)": entry["price"],
        "Unit Price (Rs/L)": round(unit_price, 2)
    })

df = pd.DataFrame(processed_records)
print(df.to_string(index=False))
