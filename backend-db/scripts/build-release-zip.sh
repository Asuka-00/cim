#!/usr/bin/env bash
# 构建内网更版包，并同步 bootstrap 到 docker/single/backend。
#
# 产出：
#   1) dist/release/app-{VERSION}.zip   — 管理页上传用（app.jar + version.json）
#   2) docker/single/backend/{app.jar,version.json} — compose 首次 build 用
#
# 用法（在 admin/backend 内）：
#   ./scripts/build-release-zip.sh [VERSION]
#
# 环境变量：
#   FRONTEND_DIR     前端工程目录，默认 <repo>/../frontend
#   OUT_DIR          zip 输出目录，默认 <repo>/dist/release
#   SKIP_FRONTEND=1  跳过前端构建
#   SYNC_DEPLOY=0    不同步 docker/single/backend
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND="$ROOT"
FRONTEND="${FRONTEND_DIR:-$ROOT/../frontend}"
OUT_DIR="${OUT_DIR:-$ROOT/dist/release}"
DEPLOY_BACKEND="$ROOT/docker/single/backend"
SYNC_DEPLOY="${SYNC_DEPLOY:-1}"
SKIP_FRONTEND="${SKIP_FRONTEND:-0}"

# 跳过 <parent> 段，否则会取到 Spring Boot 的版本号
POM_VERSION="$(
  sed -n '/<\/parent>/,$p' "$BACKEND/pom.xml" \
    | grep -m1 '<version>' | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/'
)"
RAW_VERSION="${1:-$POM_VERSION}"
VERSION="${RAW_VERSION%-SNAPSHOT}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: version must be MAJOR.MINOR.PATCH, got: $VERSION" >&2
  echo "       pass an explicit version, e.g. ./scripts/build-release-zip.sh 1.0.0" >&2
  exit 1
fi

STATIC_DIR="$BACKEND/app-boot/src/main/resources/static"
STAGE="$(mktemp -d "${TMPDIR:-/tmp}/app-release.XXXXXX")"
BUILD_ROOT=""
cleanup() {
  rm -rf "$STAGE"
  if [[ -n "$BUILD_ROOT" ]]; then
    rm -rf "$BUILD_ROOT"
  fi
  if [[ -d "$STATIC_DIR" ]]; then
    rm -rf "$STATIC_DIR"
  fi
}
trap cleanup EXIT

if [[ "$SKIP_FRONTEND" != "1" ]]; then
  if [[ ! -d "$FRONTEND" ]]; then
    echo "ERROR: frontend dir not found: $FRONTEND" >&2
    echo "       set FRONTEND_DIR=/path/to/frontend or SKIP_FRONTEND=1" >&2
    exit 1
  fi
  echo "==> build frontend ($FRONTEND)"
  (
    cd "$FRONTEND"
    if command -v pnpm >/dev/null 2>&1; then
      corepack enable >/dev/null 2>&1 || true
      pnpm install --frozen-lockfile
      pnpm build
    else
      echo "ERROR: pnpm not found" >&2
      exit 1
    fi
  )
  echo "==> copy frontend dist into boot static"
  mkdir -p "$STATIC_DIR"
  rsync -a --delete "$FRONTEND/dist/" "$STATIC_DIR/"
else
  echo "==> SKIP_FRONTEND=1, package backend jar only"
fi

# 在仓库外的副本里编译，避免 IDE/ECJ 往 workspace target/ 写入残缺 *ConverterImpl
echo "==> build backend jar (isolated tree)"
BUILD_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/app-mvn.XXXXXX")"
rsync -a \
  --exclude ".git/" \
  --exclude "**/target/" \
  --exclude "dist/" \
  --exclude "docker/single/data/" \
  --exclude "docker/single/images/" \
  "$BACKEND/" "$BUILD_ROOT/"
(
  cd "$BUILD_ROOT"
  ./mvnw -pl app-boot -am clean package -DskipTests
)

JAR_SRC="$(ls -1 "$BUILD_ROOT/app-boot/target"/app-boot-*.jar | grep -v ".original$" | head -n1)"
if [[ -z "$JAR_SRC" || ! -f "$JAR_SRC" ]]; then
  echo "ERROR: jar not found under isolated app-boot/target" >&2
  exit 1
fi

cp "$JAR_SRC" "$STAGE/app.jar"

if command -v shasum >/dev/null 2>&1; then
  JAR_SHA="$(shasum -a 256 "$STAGE/app.jar" | awk "{print \$1}")"
elif command -v sha256sum >/dev/null 2>&1; then
  JAR_SHA="$(sha256sum "$STAGE/app.jar" | awk "{print \$1}")"
else
  echo "ERROR: need shasum or sha256sum" >&2
  exit 1
fi

BUILD_TIME="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
GIT_COMMIT="$(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo unknown)"

cat >"$STAGE/version.json" <<EOF
{
  "version": "$VERSION",
  "buildTime": "$BUILD_TIME",
  "gitCommit": "$GIT_COMMIT",
  "jarSha256": "$JAR_SHA"
}
EOF

mkdir -p "$OUT_DIR"
ZIP_PATH="$OUT_DIR/app-${VERSION}.zip"
rm -f "$ZIP_PATH"
(
  cd "$STAGE"
  zip -q -9 "$ZIP_PATH" app.jar version.json
)

if [[ "$SYNC_DEPLOY" == "1" ]]; then
  if [[ ! -d "$DEPLOY_BACKEND" ]]; then
    echo "ERROR: docker/single/backend not found: $DEPLOY_BACKEND" >&2
    exit 1
  fi
  echo "==> sync bootstrap -> docker/single/backend/"
  cp -f "$STAGE/app.jar" "$DEPLOY_BACKEND/app.jar"
  cp -f "$STAGE/version.json" "$DEPLOY_BACKEND/version.json"
  rm -f "$DEPLOY_BACKEND"/app-boot-*.jar
fi

echo "==> done"
echo "    package  : $ZIP_PATH"
echo "    version  : $VERSION"
echo "    sha256   : $JAR_SHA"
if [[ "$SYNC_DEPLOY" == "1" ]]; then
  echo "    bootstrap: $DEPLOY_BACKEND/app.jar"
  echo "    next     : ./scripts/ship.sh $VERSION user@host"
fi
cat "$STAGE/version.json"
