# Hướng Dẫn Đóng Gói & Khởi Chạy Hệ Thống DAVS Bằng Docker

Tài liệu này hướng dẫn khởi chạy toàn bộ Monorepo **Hệ thống Phân tích và Trực quan hóa dữ liệu (DAVS)** gồm 3 dịch vụ:
1. **`postgres`**: Cơ sở dữ liệu PostgreSQL 16.
2. **`backend-springboot`**: REST API Spring Boot 3.3 (Java 21, Flyway, JPA, JWT Security).
3. **`frontend-react`**: Giao diện SPA React + Vite + TypeScript + Zustand + shadcn/ui (phục vụ qua Nginx Alpine).

## 1. Khởi chạy toàn bộ hệ thống (Docker Compose)

```bash
cp .env.example .env
docker compose up --build -d
```

- **Frontend (React SPA):** http://localhost:3000
- **Backend API (Spring Boot):** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html

## 2. Dừng hệ thống

```bash
docker compose down
```
