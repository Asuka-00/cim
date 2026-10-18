#!/usr/bin/env bash
# 首次/整包推送：构建 + rsync docker/single 到服务器 + compose up
#
# 用法：
#   ./scripts/ship.sh <VERSION> <user@host> [REMOTE_ROOT]
#
#   REMOTE_ROOT  远端安装根目录，默认 /opt/scaffold
#                实际部署目录为 $REMOTE_ROOT/single
#
# 示例：
#   ./scripts/ship.sh 1.0.0 root@192.168.12.1
#   ./scripts/ship.sh 1.0.0 root@192.168.12.1 /data/scaffold
#   ./scripts/ship.sh 1.0.0 root@192.168.12.1:/data/scaffold   # 兼容 user@host:path
#   SHIP_NO_START=1 ./scripts/ship.sh 1.0.0 user@host
#   SHIP_SKIP_BUILD=1 ./scripts/ship.sh 1.0.0 user@host
#
# 密码只输一次：通过 SSH ControlMaster 复用同一会话（rsync / 多次 ssh）。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_REMOTE_ROOT="/opt/scaffold"

usage() {
  cat >&2 <<'EOF'
Usage: ./scripts/ship.sh <VERSION> <user@host> [REMOTE_ROOT]

  VERSION      MAJOR.MINOR.PATCH，如 1.0.0
  user@host    SSH 目标
  REMOTE_ROOT  安装根目录，默认 /opt/scaffold（部署到 $REMOTE_ROOT/single）

Examples:
  ./scripts/ship.sh 1.0.0 root@192.168.12.1
  ./scripts/ship.sh 1.0.0 root@192.168.12.1 /data/scaffold
EOF
  exit 1
}

[[ $# -ge 2 ]] || usage
VERSION="$1"
TARGET="$2"
REMOTE_ROOT_ARG="${3:-}"

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: version must be MAJOR.MINOR.PATCH, got: $VERSION" >&2
  exit 1
fi

# 解析 user@host 与安装目录
# 支持：user@host | user@host:/path | 第三参数 /path
if [[ "$TARGET" =~ ^([^@]+@[^:]+):(.*)$ ]]; then
  REMOTE_HOST="${BASH_REMATCH[1]}"
  REMOTE_ROOT="${BASH_REMATCH[2]}"
  REMOTE_ROOT="${REMOTE_ROOT#"${REMOTE_ROOT%%[![:space:]]*}"}"
  if [[ -n "$REMOTE_ROOT_ARG" ]]; then
    echo "ERROR: 请勿同时使用 user@host:path 与第三参数 REMOTE_ROOT" >&2
    exit 1
  fi
else
  REMOTE_HOST="$TARGET"
  REMOTE_ROOT="${REMOTE_ROOT_ARG:-$DEFAULT_REMOTE_ROOT}"
fi
[[ -n "$REMOTE_ROOT" ]] || REMOTE_ROOT="$DEFAULT_REMOTE_ROOT"
# 规范化：去掉末尾 /
REMOTE_ROOT="${REMOTE_ROOT%/}"

LOCAL_DIR="$ROOT/docker/single"
REMOTE_DIR="$REMOTE_ROOT/single"

if [[ ! -d "$LOCAL_DIR" ]]; then
  echo "ERROR: missing $LOCAL_DIR" >&2
  exit 1
fi

for cmd in rsync ssh; do
  command -v "$cmd" >/dev/null || { echo "ERROR: need $cmd" >&2; exit 1; }
done

# ---- SSH 连接复用：全程只认证一次 ----
# ControlPath 受 Unix socket 路径长度限制（macOS 约 104 字节），必须尽量短
CTRL_PATH="/tmp/scaffold-cm-$$"
cleanup_ssh() {
  ssh -o ControlPath="$CTRL_PATH" -O exit "$REMOTE_HOST" 2>/dev/null || true
  rm -f "$CTRL_PATH" "${CTRL_PATH}".* 2>/dev/null || true
}
trap cleanup_ssh EXIT

# 基础 SSH 选项 + 用户附加 SHIP_SSH_OPTS（兼容 set -u 与空数组）
SSH_BASE=(
  -o "ControlMaster=auto"
  -o "ControlPath=$CTRL_PATH"
  -o "ControlPersist=yes"
  -o "StrictHostKeyChecking=accept-new"
)

run_ssh() {
  # shellcheck disable=SC2086
  ssh "${SSH_BASE[@]}" ${SHIP_SSH_OPTS:-} "$REMOTE_HOST" "$@"
}

# rsync -e 需要一条 shell 命令字符串
RSYNC_RSH="ssh -o ControlMaster=auto -o ControlPath=$CTRL_PATH -o ControlPersist=yes -o StrictHostKeyChecking=accept-new ${SHIP_SSH_OPTS:-}"

echo "==> open SSH session to $REMOTE_HOST (enter password once if needed)"
# 建立 master：前台一次认证；后续命令复用
run_ssh -fN true

if [[ "${SHIP_SKIP_BUILD:-0}" != "1" ]]; then
  echo "==> build release ($VERSION)"
  "$ROOT/scripts/build-release-zip.sh" "$VERSION"
else
  echo "==> SHIP_SKIP_BUILD=1, skip build"
  if [[ ! -f "$LOCAL_DIR/backend/app.jar" ]]; then
    echo "ERROR: $LOCAL_DIR/backend/app.jar missing; run build first" >&2
    exit 1
  fi
fi

echo "==> prepare remote dir $REMOTE_DIR"
run_ssh "mkdir -p '$REMOTE_DIR'"

echo "==> rsync $LOCAL_DIR/ -> $REMOTE_HOST:$REMOTE_DIR/"
RSYNC_EXCLUDES=(
  --exclude 'data/'
  --exclude '.env'
  --exclude '.DS_Store'
)
rsync -az --delete "${RSYNC_EXCLUDES[@]}" \
  -e "$RSYNC_RSH" \
  "$LOCAL_DIR/" "$REMOTE_HOST:$REMOTE_DIR/"

echo "==> ensure .env"
run_ssh bash -s <<EOF
set -euo pipefail
cd '$REMOTE_DIR'
if [[ ! -f .env ]]; then
  if [[ -f .env.example ]]; then
    cp .env.example .env
  else
    touch .env
  fi
else
  echo ".env already exists, keep it"
fi
EOF

if [[ "${SHIP_NO_START:-0}" == "1" ]]; then
  echo "==> SHIP_NO_START=1, skip compose up"
  echo "    remote: $REMOTE_HOST:$REMOTE_DIR"
  exit 0
fi

echo "==> docker compose up -d --build on remote"
run_ssh bash -s <<EOF
set -euo pipefail
cd '$REMOTE_DIR'
if ! command -v docker >/dev/null; then
  echo "ERROR: docker not found on remote" >&2
  exit 1
fi
docker compose up -d --build
docker compose ps
EOF

echo "==> verify remote app is up and running $VERSION"
run_ssh bash -s <<EOF || { echo "ERROR: 部署未通过校验，远端未跑起 $VERSION" >&2; exit 1; }
set -uo pipefail
cd '$REMOTE_DIR'
# 判据只看"本版本的进程启动完成"。不用 /actuator/health：那是聚合健康，
# 依赖抖动就会返回 503，应用其实完全可用，会把部署卡到超时。
for i in \$(seq 1 60); do
  # 只取最近一次启动之后的日志，否则历史成功记录会让校验误判通过
  LAST="\$(docker compose logs --no-log-prefix app 2>/dev/null \\
    | awk '/Starting app version=/{buf=""} {buf=buf \$0 ORS} END{printf "%s", buf}')"
  if grep -q "Starting app version=$VERSION " <<<"\$LAST" \\
     && grep -q "Started AppApplication" <<<"\$LAST"; then
    echo "app started on version $VERSION"
    exit 0
  fi
  if ! docker compose ps --status running --services 2>/dev/null | grep -qx app; then
    echo "app container exited:" >&2
    docker compose logs --tail=40 app >&2
    exit 1
  fi
  sleep 5
done

echo "timeout waiting for app，最近一次启动的判据命中情况:" >&2
echo "  Starting app version=$VERSION : \$(grep -c "Starting app version=$VERSION " <<<"\$LAST")" >&2
echo "  Started AppApplication : \$(grep -c "Started AppApplication" <<<"\$LAST")" >&2
docker compose logs --tail=40 app >&2
exit 1
EOF

DISPLAY_HOST="${REMOTE_HOST#*@}"
echo ""
echo "==> done"
echo "    version : $VERSION"
echo "    remote  : $REMOTE_HOST:$REMOTE_DIR"
UI_PORT="$(run_ssh "sed -n 's/^APP_PORT=//p' '$REMOTE_DIR/.env' 2>/dev/null | tail -n1" || true)"
UI_PORT="${UI_PORT:-8848}"
echo "    UI      : http://${DISPLAY_HOST}:${UI_PORT}"
echo "    later   : ./scripts/build-release-zip.sh x.y.z  # 管理页上传 zip 更版"
