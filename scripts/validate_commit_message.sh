#!/usr/bin/env bash

set -euo pipefail

message_file=${1:?"커밋 메시지 파일 경로가 필요합니다."}
subject=$(sed -n '1p' "$message_file")

if [[ "$subject" == Merge\ * || "$subject" == Revert\ \"* ]]; then
  exit 0
fi

pattern='^(feat|fix|refactor|test|docs|chore|deps|perf|build|ci|revert)(\([a-z0-9._/-]+\))?: .+'
if [[ ! "$subject" =~ $pattern ]]; then
  echo "커밋 메시지는 type(scope): 제목 형식이어야 합니다." >&2
  echo "허용 type: feat, fix, refactor, test, docs, chore, deps, perf, build, ci, revert" >&2
  exit 1
fi

if (( ${#subject} > 50 )); then
  echo "커밋 메시지 제목은 50자 이하여야 합니다. 현재 ${#subject}자입니다." >&2
  exit 1
fi
