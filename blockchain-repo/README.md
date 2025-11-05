# 🧱 BLOCKCHAIN — Cleaned & Structured Repository

Reorganized on 2025-11-05 18:11 into a standard, interview‑ready GitHub layout.

## What was done
- Gathered all Solidity contracts into `contracts/contracts/`
- Normalized Hardhat configs, scripts, and tests in `contracts/`
- Detected any frontend app and moved it under `dapp/`
- Preserved **all original files** in `original_upload_backup/`

## Quick Start (Contracts)
```bash
cd contracts
npm install
cp .env.example .env
# set: SEPOLIA_RPC_URL, PRIVATE_KEY, (optionals) TOKEN_NAME, TOKEN_SYMBOL, INITIAL_SUPPLY
npm run build
npm test
# optional: npm run deploy:sepolia
```

## Quick Start (dApp) — if present
```bash
cd dapp
npm install
npm run dev
```

> Educational/testnet only — not audited. Never commit private keys.
