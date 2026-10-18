#!/usr/bin/env bash
# 打离线安装包：compose + 预构建 linux/amd64 镜像 + 管理页更版 zip
#
# 用法：
#   ./scripts/package-offline.sh <VERSION>
# 产出：
#   dist/scaffold-single-{VERSION}.tgz
#
# 打包机需能访问 Docker Hub / GHCR。目标机只需 Docker + compose（linux/amd64）。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_DIR="$ROOT/docker/single"

[[ $# -ge 1 ]] || { echo "Usage: $0 <VERSION>" >&2; exit 1; }
VERSION="$1"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: version must be MAJOR.MINOR.PATCH" >&2
  exit 1
fi

command -v docker >/dev/null || { echo "ERROR: docker not found" >&2; exit 1; }
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
  echo "       brew 的 docker/docker-compose 只是客户端。请先启动 Docker Desktop、Colima 或 OrbStack。" >&2
  exit 1
fi

"$ROOT/scripts/build-release-zip.sh" "$VERSION"

if [[ ! -f "$COMPOSE_DIR/backend/app.jar" ]]; then
  echo "ERROR: $COMPOSE_DIR/backend/app.jar missing after build" >&2
  exit 1
fi

export DOCKER_DEFAULT_PLATFORM=linux/amd64
export APP_VERSION="$VERSION"

APP_IMAGE="scaffold-app:${VERSION}"
MID_IMAGES=(
  mysql:8.0-oraclelinux8
  redis:7.2-alpine
  ghcr.io/navikt/mock-oauth2-server:2.1.10
)

echo "==> pull middleware images (linux/amd64)"
for img in "${MID_IMAGES[@]}"; do
  if docker pull --platform linux/amd64 "$img"; then
    continue
  fi
  if docker image inspect "$img" >/dev/null 2>&1; then
    echo "WARN: pull $img failed, using local image" >&2
    continue
  fi
  echo "ERROR: pull $img failed and no local image" >&2
  exit 1
done

echo "==> build $APP_IMAGE"
(
  cd "$COMPOSE_DIR"
  "${COMPOSE[@]}" build app
)

ALL_IMAGES=("${MID_IMAGES[@]}" "$APP_IMAGE")

OUT_DIR="${OUT_DIR:-$ROOT/dist}"
mkdir -p "$OUT_DIR"
TGZ="$OUT_DIR/scaffold-single-${VERSION}.tgz"
STAGE="$(mktemp -d "${TMPDIR:-/tmp}/scaffold-offline.XXXXXX")"
trap 'rm -rf "$STAGE"' EXIT

mkdir -p "$STAGE/single/images"
rsync -a \
  --exclude 'data/' \
  --exclude '/.env' \
  --exclude '.DS_Store' \
  --exclude 'images/' \
  "$COMPOSE_DIR/" "$STAGE/single/"

if [[ ! -f "$COMPOSE_DIR/.env.example" ]]; then
  echo "ERROR: $COMPOSE_DIR/.env.example missing" >&2
  exit 1
fi
cp -f "$COMPOSE_DIR/.env.example" "$STAGE/single/.env.example"
echo "    included: single/.env.example"

printf '%s\n' "$VERSION" >"$STAGE/single/images/version"
{
  echo "# image  id"
  for img in "${ALL_IMAGES[@]}"; do
    echo "$img  $(docker image inspect --format '{{.Id}}' "$img")"
  done
} >"$STAGE/single/images/manifest.txt"

if grep -q '^APP_VERSION=' "$STAGE/single/.env.example" 2>/dev/null; then
  sed -i.bak "s/^APP_VERSION=.*/APP_VERSION=$VERSION/" "$STAGE/single/.env.example"
  rm -f "$STAGE/single/.env.example.bak"
elif grep -q '^# APP_VERSION=' "$STAGE/single/.env.example" 2>/dev/null; then
  sed -i.bak "s/^# APP_VERSION=.*/APP_VERSION=$VERSION/" "$STAGE/single/.env.example"
  rm -f "$STAGE/single/.env.example.bak"
else
  echo "APP_VERSION=$VERSION" >>"$STAGE/single/.env.example"
fi

echo "==> docker save (${#ALL_IMAGES[@]} images)"
docker save --platform linux/amd64 "${ALL_IMAGES[@]}" | gzip -1 >"$STAGE/single/images/scaffold-images.tar.gz"

if [[ -f "$ROOT/dist/release/app-${VERSION}.zip" ]]; then
  mkdir -p "$STAGE/release"
  cp "$ROOT/dist/release/app-${VERSION}.zip" "$STAGE/release/"
fi

tar -C "$STAGE" -czf "$TGZ" .
echo "==> offline package: $TGZ"
echo "    server: tar -xzf $(basename "$TGZ") && cd single && ./install.sh"
