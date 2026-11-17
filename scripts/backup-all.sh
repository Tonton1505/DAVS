#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# DAVS Full Backup Script (PostgreSQL + MinIO) - T-107
# RPO <= 24h (scheduled daily via cron/Task Scheduler)
# ============================================================================

BACKUP_ROOT="${BACKUP_ROOT:-./backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
RUN_DIR="${BACKUP_ROOT}/${TIMESTAMP}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-davs-prod-postgres}"
DB_USER="${DB_USER:-davs}"
DB_NAME="${POSTGRES_DB:-davs}"

MINIO_NETWORK="${MINIO_NETWORK:-davs_prod_network}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY must be set}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:?MINIO_SECRET_KEY must be set}"
MINIO_BUCKETS="${MINIO_BUCKETS:-workbooks datasets exports attachments}"

mkdir -p "${RUN_DIR}/postgres" "${RUN_DIR}/minio"

echo "[1/3] Backing up PostgreSQL (${DB_NAME})..."
PG_DUMP_FILE="${RUN_DIR}/postgres/davs_${TIMESTAMP}.dump"
docker exec "${POSTGRES_CONTAINER}" \
  pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc --no-owner --no-acl > "${PG_DUMP_FILE}"

echo "[2/3] Backing up MinIO buckets (${MINIO_BUCKETS})..."
ABS_MINIO_DIR="$(cd "${RUN_DIR}/minio" && pwd)"
docker run --rm --network "${MINIO_NETWORK}" \
  -v "${ABS_MINIO_DIR}:/backup" \
  -e "MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}" \
  -e "MINIO_SECRET_KEY=${MINIO_SECRET_KEY}" \
  pgsty/silo:latest /bin/sh -c "
    mc alias set src ${MINIO_ENDPOINT} \"\$MINIO_ACCESS_KEY\" \"\$MINIO_SECRET_KEY\" >/dev/null &&
    for b in ${MINIO_BUCKETS}; do
      mkdir -p \"/backup/\$b\"
      mc mirror --quiet \"src/\$b\" \"/backup/\$b\" || true
    done
  "

tar -czf "${RUN_DIR}/minio_buckets_${TIMESTAMP}.tar.gz" -C "${RUN_DIR}/minio" .
rm -rf "${RUN_DIR}/minio"

echo "[3/3] Generating SHA-256 manifest..."
(
  cd "${RUN_DIR}"
  sha256sum postgres/*.dump minio_buckets_*.tar.gz > SHA256SUMS
)

find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -mtime +"${RETENTION_DAYS}" -exec rm -rf {} + 2>/dev/null || true

echo "Full backup completed successfully: ${RUN_DIR}"
