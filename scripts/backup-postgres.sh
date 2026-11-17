#!/usr/bin/env bash
set -euo pipefail

# NFR-PERF-12 / T-107: Sao lưu PostgreSQL hằng ngày (RPO <= 24 giờ, RTO <= 4 giờ)
# Có thể chạy trực tiếp với Docker container (mặc định: davs-prod-postgres hoặc davs-postgres)
# hoặc kết nối trực tiếp qua PGHOST/PGPORT.

BACKUP_DIR="${BACKUP_DIR:-./backups/postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-davs-prod-postgres}"
DB_USER="${DB_USER:-davs}"
DB_NAME="${POSTGRES_DB:-davs}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="${BACKUP_DIR}/davs_pg_${TIMESTAMP}.dump"

mkdir -p "${BACKUP_DIR}"

if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -qx "${POSTGRES_CONTAINER}"; then
  docker exec "${POSTGRES_CONTAINER}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc --no-owner --no-acl > "${OUT_FILE}"
else
  pg_dump -U "${DB_USER}" -d "${DB_NAME}" -Fc --no-owner --no-acl > "${OUT_FILE}"
fi

sha256sum "${OUT_FILE}" > "${OUT_FILE}.sha256" 2>/dev/null || shasum -a 256 "${OUT_FILE}" > "${OUT_FILE}.sha256"

# Xóa bản sao lưu cũ hơn RETENTION_DAYS ngày
find "${BACKUP_DIR}" -type f -name "davs_pg_*.dump*" -mtime +"${RETENTION_DAYS}" -delete 2>/dev/null || true

echo "PostgreSQL backup completed: ${OUT_FILE}"
