# Root Orchestrator / Utility Dockerfile cho DAVS Monorepo
FROM alpine:3.20
WORKDIR /workspace
COPY README_DOCKER.md .
CMD ["cat", "README_DOCKER.md"]
