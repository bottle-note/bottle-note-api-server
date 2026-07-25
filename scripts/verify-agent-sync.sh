#!/usr/bin/env bash
# AGENTS.md(codex) / CLAUDE.md(Claude Code) 및 .agents / .claude 미러의 동기화를 검증한다.
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

fail=0
note() { printf '%s\n' "$1"; }

# 런타임 고유 토큰을 CLAUDE 쪽 표기로 정규화한다.
normalize() {
  sed -e 's/Codex CLI/Claude Code/g' \
    -e 's|\.agents/|.claude/|g' \
    -e 's/AGENTS\.personal\.md/CLAUDE.personal.md/g' \
    -e 's/^# AGENTS\.md$/# CLAUDE.md/' \
    "$1"
}

# 1) 지침 본문은 정규화 후 완전히 같아야 한다.
if diff_out=$(diff -u <(normalize AGENTS.md) <(normalize CLAUDE.md)); then
  note "[OK] AGENTS.md == CLAUDE.md (정규화 기준)"
else
  note "[FAIL] AGENTS.md 와 CLAUDE.md 가 어긋납니다:"
  printf '%s\n' "$diff_out"
  fail=1
fi

# 2) 스킬 세트는 바이트 단위로 같아야 한다.
if diff_out=$(diff -rq .agents/skills .claude/skills); then
  note "[OK] .agents/skills == .claude/skills"
else
  note "[FAIL] 스킬 미러가 어긋납니다:"
  printf '%s\n' "$diff_out"
  fail=1
fi

# 3) 지침 Skills 표가 참조하는 슬래시 커맨드는 실제 스킬로 존재해야 한다.
checked=0
missing=0
while read -r cmd; do
  [[ -z "$cmd" ]] && continue
  checked=$((checked + 1))
  if [[ ! -d ".claude/skills/$cmd" ]]; then
    note "[FAIL] 지침이 참조하는 /$cmd 스킬이 없습니다"
    missing=$((missing + 1))
    fail=1
  fi
done < <(grep -oE '^\| `/[a-z-]+`' CLAUDE.md | tr -d '| `/')
if [[ $checked -eq 0 ]]; then
  note "[FAIL] Skills 표에서 슬래시 커맨드를 추출하지 못했습니다 (표 형식 변경 의심)"
  fail=1
elif [[ $missing -eq 0 ]]; then
  note "[OK] Skills 표 커맨드 ${checked}개 모두 스킬 존재"
fi

# 4) 훅 설정에 머신 고유 절대경로가 박히면 다른 체크아웃에서 깨진다.
if hits=$(grep -nE '[A-Za-z]:\\|/Users/|/home/' .codex/hooks.json .claude/settings.json); then
  note "[FAIL] 훅 설정에 머신 고유 절대경로가 있습니다:"
  printf '%s\n' "$hits"
  fail=1
else
  note "[OK] 훅 설정에 절대경로 없음"
fi

# 5) codex 훅이 Claude 전용 환경변수를 참조하면 조용히 실패한다.
if hits=$(grep -n 'CLAUDE_PROJECT_DIR\|CLAUDE_CODE_REMOTE' .codex/hooks.json); then
  note "[FAIL] .codex/hooks.json 이 Claude 전용 환경변수를 참조합니다:"
  printf '%s\n' "$hits"
  fail=1
else
  note "[OK] .codex/hooks.json 에 Claude 전용 환경변수 없음"
fi

if [[ $fail -eq 0 ]]; then
  note "동기화 검증 통과"
else
  note "동기화 검증 실패"
fi
exit $fail
