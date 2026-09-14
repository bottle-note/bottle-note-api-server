#!/usr/bin/env bash
set -euo pipefail

# Orca 작업 트리 준비. 빌드와 테스트는 실행하지 않는다.
cd "${ORCA_WORKTREE_PATH:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"

echo "[orca] setup: $(pwd)"

if [[ ! -f .gitmodules ]]; then
  echo "[orca] .gitmodules 가 없어 서브모듈 초기화를 건너뜁니다."
  exit 0
fi

git submodule update --init --recursive

migration_dir="git.environment-variables/storage/db/migration"
shopt -s nullglob
sqls=("${migration_dir}"/*.sql)
if (( ${#sqls[@]} == 0 )); then
  echo "[orca] Flyway SQL 이 ${migration_dir} 에 없습니다." >&2
  echo "[orca] git.environment-variables 서브모듈 초기화를 확인하세요." >&2
  exit 1
fi

echo "[orca] git.environment-variables 준비 완료 (${#sqls[@]} sql)"
