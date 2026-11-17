#!/usr/bin/env bash
set -euo pipefail

# Generate a self-signed TLS certificate for staging/testing DAVS HTTPS reverse proxy.
# For production, replace infra/certs/tls.crt and infra/certs/tls.key with CA-signed certs (e.g. Let's Encrypt).

OUT_DIR="${1:-infra/certs}"
DOMAIN="${2:-localhost}"

mkdir -p "$OUT_DIR"

openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout "$OUT_DIR/tls.key" \
  -out "$OUT_DIR/tls.crt" \
  -subj "/CN=${DOMAIN}/O=DAVS/C=VN" \
  -addext "subjectAltName=DNS:${DOMAIN},DNS:davs.local,IP:127.0.0.1"

chmod 600 "$OUT_DIR/tls.key"
chmod 644 "$OUT_DIR/tls.crt"

echo "Generated TLS certificate:"
echo "  CRT: $OUT_DIR/tls.crt"
echo "  KEY: $OUT_DIR/tls.key"
