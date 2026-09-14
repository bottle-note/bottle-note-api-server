#!/usr/bin/env bash
set -euo pipefail

: "${REPOSITORY:?REPOSITORY is required}"
: "${RELEASE_KEY:?RELEASE_KEY is required}"
: "${SOURCE_SHA:?SOURCE_SHA is required}"
: "${RELEASE_TYPE:?RELEASE_TYPE is required}"
: "${RUN_URL:?RUN_URL is required}"
: "${DEPLOYED_SERVICES:?DEPLOYED_SERVICES is required}"
: "${GITHUB_STEP_SUMMARY:?GITHUB_STEP_SUMMARY is required}"

verify_attempts="${DEPLOYED_TAG_VERIFY_ATTEMPTS:-5}"
verify_delay_seconds="${DEPLOYED_TAG_VERIFY_DELAY_SECONDS:-2}"

json_object_fields() {
  JSON_VALUE="$1" python3 -c '
import json, os
obj = json.loads(os.environ["JSON_VALUE"]).get("object") or {}
print(obj.get("type") or "-", obj.get("sha") or "-")
'
}

resolve_tag_ref() {
  local ref_json="$1" object_type object_sha tag_json resolved
  read -r object_type object_sha <<< "$(json_object_fields "$ref_json")"
  resolved="$object_sha"
  if [[ "$object_type" == "tag" ]]; then
    tag_json="$(gh api "repos/${REPOSITORY}/git/tags/${object_sha}")"
    read -r object_type resolved <<< "$(json_object_fields "$tag_json")"
  fi
  [[ "$resolved" =~ ^[0-9a-f]{40}$ ]] || {
    echo "::error::deployed tag does not resolve to a commit SHA" >&2
    return 1
  }
  printf '%s' "$resolved"
}

is_not_found() {
  [[ "$1" == *"HTTP 404"* || "$1" == *"Not Found"* ]]
}

create_deployed_tag() {
  local service="$1" key="$2" source_sha="$3" release_type="$4" run_url="$5"
  local tag_name endpoint response tag_message tag_object_json tag_object_sha
  local ref_json resolved attempt

  [[ "$source_sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "::error::deployed source SHA is invalid: ${source_sha}" >&2
    return 1
  }
  [[ "$key" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}/[1-9][0-9]*$ ]] || {
    echo "::error::release key is invalid: ${key}" >&2
    return 1
  }

  tag_name="deployed/${service}/${key}"
  endpoint="repos/${REPOSITORY}/git/ref/tags/${tag_name}"

  if ref_json="$(gh api "$endpoint" 2>&1)"; then
    resolved="$(resolve_tag_ref "$ref_json")"
    [[ "$resolved" == "$source_sha" ]] || {
      echo "::error::deployed tag ${tag_name} resolves to ${resolved}, expected ${source_sha}" >&2
      return 1
    }
    echo "- Reused \`${tag_name}\` at \`${source_sha}\`" >> "$GITHUB_STEP_SUMMARY"
    return 0
  fi
  is_not_found "$ref_json" || {
    echo "::error::unable to confirm deployed tag is absent: ${tag_name}" >&2
    return 1
  }

  tag_message="$(printf 'deployed %s %s (%s)\nsource-sha: %s\nrun: %s' \
    "$service" "$key" "$release_type" "$source_sha" "$run_url")"
  tag_object_json="$(gh api -X POST "repos/${REPOSITORY}/git/tags" \
    -f tag="$tag_name" \
    -f object="$source_sha" \
    -f type=commit \
    -f message="$tag_message")"
  tag_object_sha="$(JSON_VALUE="$tag_object_json" python3 -c 'import json, os; print(json.loads(os.environ["JSON_VALUE"]).get("sha") or "-")')"
  [[ "$tag_object_sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "::error::failed to create an annotated tag object for ${tag_name}" >&2
    return 1
  }

  if ! response="$(gh api -X POST "repos/${REPOSITORY}/git/refs" \
    -f ref="refs/tags/${tag_name}" \
    -f sha="$tag_object_sha" 2>&1)"; then
    echo "::warning::deployed tag ref creation returned non-success; verifying remote state: ${tag_name}" >&2
  fi

  for ((attempt = 1; attempt <= verify_attempts; attempt++)); do
    if ref_json="$(gh api "$endpoint" 2>&1)"; then
      resolved="$(resolve_tag_ref "$ref_json")"
      [[ "$resolved" == "$source_sha" ]] || {
        echo "::error::deployed tag ${tag_name} resolves to ${resolved}, expected ${source_sha}" >&2
        return 1
      }
      echo "- Tagged \`${tag_name}\` at \`${source_sha}\`" >> "$GITHUB_STEP_SUMMARY"
      return 0
    fi
    is_not_found "$ref_json" || {
      echo "::error::unable to verify deployed tag ${tag_name}" >&2
      return 1
    }
    ((attempt == verify_attempts)) || sleep "$verify_delay_seconds"
  done

  echo "::error::deployed tag ${tag_name} was not readable after ${verify_attempts} attempts" >&2
  return 1
}

echo "## Deployed source tagged (${RELEASE_TYPE})" >> "$GITHUB_STEP_SUMMARY"
for service in $DEPLOYED_SERVICES; do
  create_deployed_tag "$service" "$RELEASE_KEY" "$SOURCE_SHA" "$RELEASE_TYPE" "$RUN_URL"
done
