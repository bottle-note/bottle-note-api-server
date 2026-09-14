#!/usr/bin/env python3
import json
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/seal_deployed_source.sh"
DELETE_SCRIPT = ROOT / ".github/scripts/delete_release_branch.sh"
SOURCE = "a" * 40
OTHER = "b" * 40
KEY = "2026-09-14/2"

FAKE_GH = r'''#!/usr/bin/env python3
import hashlib, json, os, sys
from pathlib import Path
state_path = Path(os.environ["FAKE_GH_STATE"])
state = json.loads(state_path.read_text())
args = sys.argv[1:]
assert args[0] == "api", args
method = "GET"
if "-X" in args:
    method = args[args.index("-X") + 1]
endpoint = next((a for a in args[1:] if a.startswith("repos/")), "")
include = "--include" in args

def save():
    state_path.write_text(json.dumps(state))

def not_found():
    if include:
        print("HTTP/2 404 Not Found", file=sys.stderr)
    else:
        print("gh: Not Found (HTTP 404)", file=sys.stderr)
    sys.exit(1)

if method == "GET" and "/git/ref/tags/" in endpoint:
    tag = endpoint.split("/git/ref/tags/", 1)[1]
    target = state["refs"].get(tag)
    if target is None:
        not_found()
    remaining = state.get("transient_reads", {}).get(tag, 0)
    if remaining > 0:
        state["transient_reads"][tag] = remaining - 1
        save()
        not_found()
    print(json.dumps({"object": {"type": "tag", "sha": hashlib.sha1(tag.encode()).hexdigest()}}))
    sys.exit(0)

if method == "GET" and "/git/tags/" in endpoint:
    obj = endpoint.split("/git/tags/", 1)[1]
    tag = next(tag for tag in state["refs"] if hashlib.sha1(tag.encode()).hexdigest() == obj)
    print(json.dumps({"object": {"type": "commit", "sha": state["refs"][tag]}}))
    sys.exit(0)

if method == "GET" and "/git/ref/heads/" in endpoint:
    branch = endpoint.split("/git/ref/heads/", 1)[1]
    if branch not in state.get("branches", []):
        not_found()
    print(json.dumps({"ref": "refs/heads/" + branch}))
    sys.exit(0)

if method == "DELETE" and "/git/refs/heads/" in endpoint:
    branch = endpoint.split("/git/refs/heads/", 1)[1]
    if branch not in state.get("branches", []):
        not_found()
    state["branches"].remove(branch)
    save()
    print("{}")
    sys.exit(0)

if method == "POST" and endpoint.endswith("/git/tags"):
    tag = args[args.index("-f") + 1].split("=", 1)[1]
    object_arg = next(a for a in args if a.startswith("object="))
    state["pending"] = {"tag": tag, "source": object_arg.split("=", 1)[1]}
    save()
    print(json.dumps({"sha": hashlib.sha1(tag.encode()).hexdigest()}))
    sys.exit(0)

if method == "POST" and endpoint.endswith("/git/refs"):
    pending = state.pop("pending")
    state["refs"][pending["tag"]] = pending["source"]
    state.setdefault("transient_reads", {})[pending["tag"]] = int(os.environ.get("FAKE_TRANSIENT_READS", "0"))
    save()
    if os.environ.get("FAKE_REF_POST_LOST") == "1":
        print("gh: Reference already exists (HTTP 422)", file=sys.stderr)
        sys.exit(1)
    print("{}")
    sys.exit(0)

raise SystemExit(f"unsupported fake gh call: {args}")
'''

class SealDeployedSourceTest(unittest.TestCase):
    def run_script(self, refs, services="product admin", transient_reads=0, ref_post_lost=False):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            fake_bin = tmp_path / "bin"
            fake_bin.mkdir()
            gh = fake_bin / "gh"
            gh.write_text(FAKE_GH)
            gh.chmod(0o755)
            state = tmp_path / "state.json"
            state.write_text(json.dumps({"refs": refs, "transient_reads": {}}))
            summary = tmp_path / "summary.md"
            env = os.environ | {
                "PATH": f"{fake_bin}:{os.environ['PATH']}",
                "FAKE_GH_STATE": str(state),
                "FAKE_TRANSIENT_READS": str(transient_reads),
                "FAKE_REF_POST_LOST": "1" if ref_post_lost else "0",
                "GITHUB_STEP_SUMMARY": str(summary),
                "REPOSITORY": "bottle-note/bottle-note-api-server",
                "RELEASE_KEY": KEY,
                "SOURCE_SHA": SOURCE,
                "RELEASE_TYPE": "standard",
                "RUN_URL": "https://example.test/run/1",
                "DEPLOYED_SERVICES": services,
                "DEPLOYED_TAG_VERIFY_ATTEMPTS": "4",
                "DEPLOYED_TAG_VERIFY_DELAY_SECONDS": "0",
            }
            result = subprocess.run(["bash", str(SCRIPT)], env=env, text=True, capture_output=True)
            return result, json.loads(state.read_text()), summary.read_text() if summary.exists() else ""

    def test_existing_matching_tag_is_reused_and_missing_service_is_created(self):
        product = f"deployed/product/{KEY}"
        admin = f"deployed/admin/{KEY}"
        result, state, summary = self.run_script({product: SOURCE})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(state["refs"][admin], SOURCE)
        self.assertIn(f"Reused `{product}`", summary)
        self.assertIn(f"Tagged `{admin}`", summary)

    def test_existing_tag_with_different_source_fails(self):
        product = f"deployed/product/{KEY}"
        result, state, _ = self.run_script({product: OTHER}, services="product")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(f"expected {SOURCE}", result.stderr)
        self.assertEqual(state["refs"][product], OTHER)

    def test_new_tag_verification_retries_transient_404(self):
        frontend = f"deployed/frontend/{KEY}"
        result, state, _ = self.run_script({}, services="frontend", transient_reads=2)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(state["refs"][frontend], SOURCE)

    def test_ref_post_response_loss_recovers_by_reading_matching_tag(self):
        admin = f"deployed/admin/{KEY}"
        result, state, _ = self.run_script({}, services="admin", ref_post_lost=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(state["refs"][admin], SOURCE)


class DeleteReleaseBranchTest(unittest.TestCase):
    def run_script(self, branch, release_type="standard", existing=True):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_path = Path(tmp)
            fake_bin = tmp_path / "bin"
            fake_bin.mkdir()
            gh = fake_bin / "gh"
            gh.write_text(FAKE_GH)
            gh.chmod(0o755)
            state = tmp_path / "state.json"
            state.write_text(json.dumps({"refs": {}, "branches": [branch] if existing else []}))
            env = os.environ | {
                "PATH": f"{fake_bin}:{os.environ['PATH']}",
                "FAKE_GH_STATE": str(state),
                "REPOSITORY": "bottle-note/bottle-note-api-server",
                "RELEASE_BRANCH": branch,
                "RELEASE_TYPE": release_type,
            }
            result = subprocess.run(["bash", str(DELETE_SCRIPT)], env=env, text=True, capture_output=True)
            return result, json.loads(state.read_text())

    def test_existing_release_branch_is_deleted(self):
        branch = f"releases/{KEY}"
        result, state = self.run_script(branch)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn(branch, state["branches"])

    def test_missing_release_branch_is_already_clean(self):
        result, _ = self.run_script(f"releases/{KEY}", existing=False)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_wrong_branch_prefix_is_rejected(self):
        result, _ = self.run_script(f"hotfixes/{KEY}")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("refusing to delete", result.stderr)

if __name__ == "__main__":
    unittest.main()
