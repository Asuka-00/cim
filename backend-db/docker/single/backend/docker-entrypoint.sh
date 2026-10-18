#!/bin/sh
# 在线更版入口：始终从 $APP_RELEASE_HOME/current/app.jar 启动。
#
# 镜像内 /bootstrap 代表"整包推送"的权威版本：
#   - 无 current            -> 安装 bootstrap 并指向它（首次启动）
#   - bootstrap jar 有变化  -> 安装并接管 current（ship.sh 推了新镜像）
#   - bootstrap jar 无变化  -> 不动 current（保留管理页在线激活的版本）
set -eu

APP_HOME="${APP_RELEASE_HOME:-/opt/scaffold}"
BOOTSTRAP_DIR="${APP_BOOTSTRAP_DIR:-/bootstrap}"
JAVA_OPTS="${JAVA_OPTS:-}"

mkdir -p "$APP_HOME/releases" "$APP_HOME/incoming"

jar_sha() {
  if [ ! -f "$1" ]; then
    echo "-"
    return 0
  fi
  sha256sum "$1" | awk '{print $1}'
}

if [ -f "$BOOTSTRAP_DIR/app.jar" ] && [ -f "$BOOTSTRAP_DIR/version.json" ]; then
  VERSION="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$BOOTSTRAP_DIR/version.json" | head -n1)"
  if [ -z "$VERSION" ]; then
    echo "ERROR: cannot parse version from $BOOTSTRAP_DIR/version.json" >&2
    exit 1
  fi
  TARGET="$APP_HOME/releases/$VERSION"
  if [ "$(jar_sha "$BOOTSTRAP_DIR/app.jar")" != "$(jar_sha "$TARGET/app.jar")" ]; then
    echo "Installing bootstrap release version=$VERSION"
    mkdir -p "$TARGET"
    cp -f "$BOOTSTRAP_DIR/app.jar" "$TARGET/app.jar.tmp"
    mv -f "$TARGET/app.jar.tmp" "$TARGET/app.jar"
    cp -f "$BOOTSTRAP_DIR/version.json" "$TARGET/version.json"
    ln -sfn "releases/$VERSION" "$APP_HOME/current"
  fi
elif [ ! -e "$APP_HOME/current/app.jar" ]; then
  echo "ERROR: $APP_HOME/current missing and bootstrap package not found at $BOOTSTRAP_DIR" >&2
  exit 1
fi

if [ ! -f "$APP_HOME/current/app.jar" ]; then
  echo "ERROR: $APP_HOME/current/app.jar not found" >&2
  exit 1
fi

# 启动前自证：版本号与 jar 校验和必须与 version.json 一致，避免跑到陈旧/损坏的包
RUNNING_VERSION="unknown"
RUNNING_SHA="$(jar_sha "$APP_HOME/current/app.jar")"
if [ -f "$APP_HOME/current/version.json" ]; then
  RUNNING_VERSION="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$APP_HOME/current/version.json" | head -n1)"
  EXPECTED_SHA="$(sed -n 's/.*"jarSha256"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$APP_HOME/current/version.json" | head -n1)"
  if [ -n "$EXPECTED_SHA" ] && [ "$EXPECTED_SHA" != "$RUNNING_SHA" ]; then
    echo "ERROR: app.jar sha256 mismatch for version=$RUNNING_VERSION" >&2
    echo "       expected=$EXPECTED_SHA" >&2
    echo "       actual  =$RUNNING_SHA" >&2
    exit 1
  fi
fi

echo "Starting app version=$RUNNING_VERSION sha256=$RUNNING_SHA from $APP_HOME/current/app.jar"
# shellcheck disable=SC2086
exec java $JAVA_OPTS -Dapp.running.version="$RUNNING_VERSION" -jar "$APP_HOME/current/app.jar"
