#!/usr/bin/env bash
set -euo pipefail
# NFR-PERF-12: Sao lưu hằng ngày; RPO <= 24 giờ, RTO <= 4 giờ
pg_dump -U "${POSTGRES_USER:-davs_user}" "${POSTGRES_DB:-davs_db}" > "backup_$(date +%Y%m%d_%H%M%S).sql"
