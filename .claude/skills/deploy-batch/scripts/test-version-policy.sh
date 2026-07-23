#!/usr/bin/env bash
set -euo pipefail

# 로컬 Git fixture로 배포 버전 정책을 회귀 검증한다.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
AGENTS_SKILL="$PROJECT_ROOT/.agents/skills/deploy-batch"
CLAUDE_SKILL="$PROJECT_ROOT/.claude/skills/deploy-batch"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

fail() {
    echo "[FAIL] $1" >&2
    exit 1
}

assert_output() {
    local output=$1
    local expected=$2
    grep -Fqx "$expected" <<< "$output" ||
        fail "출력 누락: $expected"
}

expect_failure() {
    if "$@" >"$TEMP_DIR/expected-failure.out" 2>&1; then
        fail "실패해야 하는 명령이 성공했습니다: $*"
    fi
}

write_overlay() {
    local repository=$1
    local environment=$2
    local tag=$3
    local overlay_dir="$repository/deploy/overlays/$environment"

    mkdir -p "$overlay_dir"
    printf '%s\n' \
        'images:' \
        '- name: bottlenote-batch' \
        '  newName: docker-registry.bottle-note.com/bottlenote-batch' \
        "  newTag: $tag" \
        > "$overlay_dir/kustomization.yaml"
}

commit_fixture() {
    local message=$1

    git -C "$TEMP_DIR/seed" add deploy
    git -C "$TEMP_DIR/seed" commit -q -m "$message"
    git -C "$TEMP_DIR/seed" push -q origin main
}

git init -q --bare "$TEMP_DIR/remote.git"
git init -q -b main "$TEMP_DIR/seed"
git -C "$TEMP_DIR/seed" config user.name "Batch Version Test"
git -C "$TEMP_DIR/seed" config user.email "batch-version-test@example.com"
git -C "$TEMP_DIR/seed" remote add origin "$TEMP_DIR/remote.git"

write_overlay "$TEMP_DIR/seed" production batch_1.0.6
write_overlay "$TEMP_DIR/seed" development batch_1.0.6
commit_fixture "initial tags"
git clone -q "$TEMP_DIR/remote.git" "$TEMP_DIR/environment"

output=$(ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh")
assert_output "$output" "PRODUCTION_VERSION=1.0.6"
assert_output "$output" "DEVELOPMENT_VERSION=1.0.6"
assert_output "$output" "BASE_VERSION=1.0.6"
assert_output "$output" "RECOMMENDED_VERSION=1.0.7"

write_overlay "$TEMP_DIR/seed" production batch_1.9.9
write_overlay "$TEMP_DIR/seed" development batch_2.0.0
commit_fixture "different tags"

output=$(ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh" 2.1.0)
assert_output "$output" "BASE_VERSION=2.0.0"
assert_output "$output" "RECOMMENDED_VERSION=2.0.1"
assert_output "$output" "CANDIDATE_VERSION=2.1.0"
expect_failure env ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh" 2.0.0
expect_failure env ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh" 2.0.1-rc.1

write_overlay "$TEMP_DIR/seed" production batch_invalid
commit_fixture "invalid tag"
expect_failure env ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh"

write_overlay "$TEMP_DIR/seed" production batch_2.0.0
write_overlay "$TEMP_DIR/seed" development other_2.0.0
commit_fixture "missing batch tag"
expect_failure env ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh"

git -C "$TEMP_DIR/environment" remote set-url origin "$TEMP_DIR/missing.git"
expect_failure env ENV_REPO_DIR="$TEMP_DIR/environment" "$SCRIPT_DIR/check-version.sh"

[[ "$("$SCRIPT_DIR/bump-version.sh" 1.0.6 --patch --value-only)" == "1.0.7" ]] ||
    fail "patch 증가 실패"
[[ "$("$SCRIPT_DIR/bump-version.sh" 1.0.6 --minor --value-only)" == "1.1.0" ]] ||
    fail "minor 증가 실패"
[[ "$("$SCRIPT_DIR/bump-version.sh" 1.0.6 --major --value-only)" == "2.0.0" ]] ||
    fail "major 증가 실패"

for script in build-image.sh push-image.sh update-kustomize.sh; do
    expect_failure "$SCRIPT_DIR/$script"
    expect_failure "$SCRIPT_DIR/$script" 1.0.7-rc.1 --dry-run
done
"$SCRIPT_DIR/build-image.sh" 1.0.7 --dry-run --skip-gradle >/dev/null
"$SCRIPT_DIR/push-image.sh" 1.0.7 --dry-run >/dev/null
"$SCRIPT_DIR/update-kustomize.sh" 1.0.7 both --dry-run >/dev/null

for script in "$AGENTS_SKILL"/scripts/*.sh "$CLAUDE_SKILL"/scripts/*.sh; do
    bash -n "$script"
done

obsolete_pattern='VERSION_''FILE|bottlenote-batch/VER''SION'
if grep -R -E "$obsolete_pattern" "$AGENTS_SKILL" "$CLAUDE_SKILL"; then
    fail "VERSION 파일 참조가 남아 있습니다"
fi
version_file="$PROJECT_ROOT/bottlenote-batch/VER""SION"
[[ ! -e "$version_file" ]] ||
    fail "bottlenote-batch/VER""SION 파일이 남아 있습니다"

workflow="$PROJECT_ROOT/.github/workflows/deploy_batch.yml"
grep -A3 '^      version:' "$workflow" | grep -q 'required: true' ||
    fail "workflow required version input이 없습니다"
grep -q 'INPUT_VERSION:.*inputs.version' "$workflow" ||
    fail "workflow version input 연결이 없습니다"
[[ "$(grep -c 'docker manifest inspect' "$workflow")" -eq 2 ]] ||
    fail "immutable image 검사는 두 단계에 있어야 합니다"

find "$AGENTS_SKILL" -type f | sed "s#^$AGENTS_SKILL/##" | sort > "$TEMP_DIR/agents-files"
find "$CLAUDE_SKILL" -type f | sed "s#^$CLAUDE_SKILL/##" | sort > "$TEMP_DIR/claude-files"
cmp -s "$TEMP_DIR/agents-files" "$TEMP_DIR/claude-files" ||
    fail "deploy-batch 복제본 파일 목록이 다릅니다"

while IFS= read -r relative_path; do
    agents_hash=$(shasum -a 256 "$AGENTS_SKILL/$relative_path" | awk '{print $1}')
    claude_hash=$(shasum -a 256 "$CLAUDE_SKILL/$relative_path" | awk '{print $1}')
    [[ "$agents_hash" == "$claude_hash" ]] ||
        fail "deploy-batch 복제본 SHA-256 불일치: $relative_path"
done < "$TEMP_DIR/agents-files"

echo "PASS: batch version policy"
