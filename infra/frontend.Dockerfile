# ============================================================================
# Stage 1: Build React + TypeScript + Vite SPA
# ============================================================================
FROM node:20-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY frontend/ ./
RUN npm run build

# ============================================================================
# Stage 2: Serve static bundle and proxy /api via Nginx
# ============================================================================
FROM nginx:1.27-alpine AS runtime

COPY infra/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
