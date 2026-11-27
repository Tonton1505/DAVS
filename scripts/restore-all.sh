#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# DAVS Full Restore Script (PostgreSQL + MinIO) - T-107
# RTO <= 4h
# Usage: ./scripts/restore-all.sh <backup_run_dir>
# ============================================================================

RUN_DIR="${1:?Usage: $0 <backup_run_dir>}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-davs-prod-postgres}"
DB_USER="${DB_USER:-davs}"
DB_NAME="${POSTGRES_DB:-davs}"

MINIO_NETWORK="${MINIO_NETWORK:-davs_prod_network}"
MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:?MINIO_ACCESS_KEY must be set}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:?MINIO_SECRET_KEY must be set}"
MINIO_BUCKETS="${MINIO_BUCKETS:-workbooks datasets exports attachments}"

echo "[1/4] Verifying backup checksums..."
(cd "${RUN_DIR}" && sha256sum -c SHA256SUMS)

PG_DUMP_FILE="$(ls "${RUN_DIR}"/postgres/*.dump | head -n 1)"
MINIO_ARCHIVE="$(ls "${RUN_DIR}"/minio_buckets_*.tar.gz | head -n 1)"

echo "[2/4] Restoring PostgreSQL from ${PG_DUMP_FILE}..."
docker exec -i "${POSTGRES_CONTAINER}" \
  pg_restore -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists --no-owner --no-acl < "${PG_DUMP_FILE}"

echo "[3/4] Restoring MinIO buckets from ${MINIO_ARCHIVE}..."
TMP_MINIO_DIR="${RUN_DIR}/.tmp_restore"
mkdir -p "${TMP_MINIO_DIR}"
trap 'rm -rf "${TMP_MINIO_DIR}"' EXIT
tar -xzf "${MINIO_ARCHIVE}" -C "${TMP_MINIO_DIR}"
ABS_TMP_MINIO_DIR="$(cd "${TMP_MINIO_DIR}" && (pwd -W 2>/dev/null || pwd))"

docker run --rm --network "${MINIO_NETWORK}" \
  -v "${ABS_TMP_MINIO_DIR}:/restore:ro" \
  -e "MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}" \
  -e "MINIO_SECRET_KEY=${MINIO_SECRET_KEY}" \
  pgsty/silo:latest /bin/sh -c "
    mc alias set dst ${MINIO_ENDPOINT} \"\$MINIO_ACCESS_KEY\" \"\$MINIO_SECRET_KEY\" >/dev/null &&
    for b in ${MINIO_BUCKETS}; do
      mc mb --ignore-existing \"dst/\$b\" >/dev/null
      if [ -d \"/restore/\$b\" ]; then
        mc mirror --overwrite --quiet \"/restore/\$b\" \"dst/\$b\"
      fi
    done
  "

echo "[4/4] Verifying restored PostgreSQL tables..."
docker exec "${POSTGRES_CONTAINER}" \
  psql -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT count(*) AS table_count FROM information_schema.tables WHERE table_schema = 'public';"

echo "Restore completed successfully from ${RUN_DIR}"
