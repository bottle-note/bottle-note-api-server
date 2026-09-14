#!/usr/bin/env bash
# 릴리즈 파이프라인의 공통 블록이 세 서비스 레포에서 같은지 확인한다.
# 블록은 워크플로 안에서 "# >>> common <name>" 과 "# <<< common <name>" 으로 구분한다.
# 기대 해시는 pipeline_blocks.sha256 에 있으며, 한 레포에서 블록을 고치면 세 레포를 모두 고쳐야 통과한다.
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

expected_file=".github/scripts/pipeline_blocks.sha256"
workflow_dir=".github/workflows"

extract_block() {
  local name="$1" file="$2"
  awk -v marker="$name" '
    index($0, ">>> common " marker) { capture = 1 }
    capture { print }
    index($0, "<<< common " marker) { capture = 0 }
  ' "$file"
}

block_hash() {
  local name="$1" out="" file
  for file in "$workflow_dir"/*.yml; do
    out+="$(extract_block "$name" "$file")"
  done
  [[ -n "$out" ]] || return 1
  printf '%s' "$out" | shasum -a 256 | cut -d' ' -f1
}

status=0
while read -r expected name; do
  [[ -n "${name:-}" ]] || continue
  if ! actual="$(block_hash "$name")"; then
    echo "MISSING  ${name}: 이 레포의 워크플로에서 블록을 찾지 못했다"
    status=1
    continue
  fi
  if [[ "$actual" == "$expected" ]]; then
    echo "OK       ${name}"
  else
    echo "MISMATCH ${name}"
    echo "         expected ${expected}"
    echo "         actual   ${actual}"
    status=1
  fi
done < <(grep -v '^\s*#' "$expected_file" | grep -v '^\s*$')

exit "$status"
