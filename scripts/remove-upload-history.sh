#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v git-filter-repo >/dev/null 2>&1; then
  echo "git-filter-repo 未安装，无法自动清理历史。"
  echo "请先安装后再执行：brew install git-filter-repo"
  exit 1
fi

if [[ -n "$(git status --short)" ]]; then
  echo "工作区不干净，请先提交或暂存当前改动，再执行历史清理。"
  exit 1
fi

backup_branch="codex/backup-before-upload-history-clean-$(date +%Y%m%d%H%M%S)"
git branch "$backup_branch"

echo "已创建备份分支: $backup_branch"
echo "开始清理 uploads 历史..."

git filter-repo \
  --path uploads \
  --path sism-main/uploads \
  --invert-paths \
  --force

cat <<EOF

历史清理完成。接下来请确认结果，然后强制推送相关分支：
  git push --force-with-lease origin <branch>

如果需要回滚，可切回备份分支：
  git switch $backup_branch
EOF
