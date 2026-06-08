#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
WALLET="${WALLET:-0x00new-blacklist-bad0}"

printf '1) Login as demo analyst\n'
LOGIN_RESPONSE=$(curl -sS -X POST "$API_BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"analyst@chainguard.demo","password":"demo-password"}')
TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
printf '%s\n\n' "$LOGIN_RESPONSE"

if [ -z "$TOKEN" ]; then
  echo 'Failed to extract JWT token from login response.' >&2
  exit 1
fi

printf '2) Evaluate wallet risk\n'
RISK_RESPONSE=$(curl -sS "$API_BASE/api/risk/wallets/$WALLET" \
  -H "Authorization: Bearer $TOKEN")
printf '%s\n\n' "$RISK_RESPONSE"

RISK_SCORE=$(printf '%s' "$RISK_RESPONSE" | sed -n 's/.*"riskScore":\([0-9]*\).*/\1/p')
RISK_LEVEL=$(printf '%s' "$RISK_RESPONSE" | sed -n 's/.*"riskLevel":"\([^"]*\)".*/\1/p')

printf '3) List AML rules\n'
curl -sS "$API_BASE/api/rules" \
  -H "Authorization: Bearer $TOKEN"
printf '\n\n'

printf '4) Create compliance case\n'
CASE_RESPONSE=$(curl -sS -X POST "$API_BASE/api/cases" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"walletAddress\":\"$WALLET\",\"title\":\"$RISK_LEVEL wallet investigation\",\"riskScore\":$RISK_SCORE,\"riskLevel\":\"$RISK_LEVEL\"}")
printf '%s\n\n' "$CASE_RESPONSE"

CASE_ID=$(printf '%s' "$CASE_RESPONSE" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

printf '5) Generate AI investigation summary\n'
curl -sS -X POST "$API_BASE/api/ai/cases/$CASE_ID/summary" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"walletAddress\":\"$WALLET\",\"riskScore\":$RISK_SCORE,\"riskLevel\":\"$RISK_LEVEL\",\"triggeredRules\":[\"BLACKLIST_EXPOSURE\",\"HIGH_FREQUENCY_TRANSFER\"],\"analystNotes\":[\"Counterparty appears on blacklist dataset\"]}"
printf '\n'
