#!/usr/bin/env bash
# 离线包一键启动（在 docker/single 目录执行，目标机无需外网）
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v docker >/dev/null; then
  echo "ERROR: docker not found" >&2
  exit 1
fi
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null; then
  COMPOSE=(docker-compose)
else
  echo "ERROR: need docker compose plugin or docker-compose" >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "ERROR: docker daemon not running (cannot reach docker.sock)" >&2
  exit 1
fi

IMAGES_TAR="images/scaffold-images.tar.gz"
if [[ ! -f "$IMAGES_TAR" ]]; then
  echo "ERROR: $IMAGES_TAR missing. This script is for the offline package." >&2
  echo "       Online: from backend root run ./scripts/ship.sh, or here: docker compose up -d --build" >&2
  exit 1
fi

echo "==> docker load $IMAGES_TAR"
docker load -i "$IMAGES_TAR"

if [[ ! -f .env.example ]]; then
  echo "ERROR: .env.example missing from this package" >&2
  exit 1
fi
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "created .env from .env.example — adjust host ports if they conflict"
else
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ "$line" =~ ^[A-Z_][A-Z0-9_]+= ]] || continue
    key="${line%%=*}"
    if ! grep -q "^${key}=" .env; then
      echo "$line" >> .env
      echo "added $key to .env from .env.example"
    fi
  done < .env.example
fi

VERSION=""
if [[ -f images/version ]]; then
  VERSION="$(tr -d '[:space:]' <images/version)"
elif [[ -f backend/version.json ]]; then
  VERSION="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' backend/version.json | head -n1)"
fi

if [[ -n "$VERSION" ]]; then
  if grep -q '^APP_VERSION=' .env 2>/dev/null; then
    sed -i.bak "s/^APP_VERSION=.*/APP_VERSION=$VERSION/" .env
    rm -f .env.bak
  else
    echo "APP_VERSION=$VERSION" >>.env
  fi
fi

if ! docker image inspect "scaffold-app:${VERSION:-latest}" >/dev/null 2>&1; then
  echo "ERROR: image scaffold-app:${VERSION:-latest} not found after docker load" >&2
  exit 1
fi

echo "==> ${COMPOSE[*]} up -d --pull never"
"${COMPOSE[@]}" up -d --pull never
"${COMPOSE[@]}" ps
HOST_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
UI_PORT=8848
if [[ -f .env ]] && grep -q '^APP_PORT=' .env; then
  UI_PORT="$(sed -n 's/^APP_PORT=//p' .env | tail -n1 | tr -d '[:space:]')"
  UI_PORT="${UI_PORT:-8848}"
fi
echo "UI: http://${HOST_IP}:${UI_PORT}"
