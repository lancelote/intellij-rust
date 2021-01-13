# TODO: think about using library for GitHub API
import json
from typing import Dict, Optional
from urllib.request import Request, urlopen

from common import get_patch_version


def get_current_milestone(repo: str, patch_version: Optional[int] = None) -> Dict:
    response = urlopen(f"https://api.github.com/repos/{repo}/milestones")
    milestones = json.load(response)
    if patch_version is None:
        patch_version = get_patch_version()
    milestone_version = f"v{patch_version}"
    result = next((milestone for milestone in milestones if milestone["title"] == milestone_version), None)
    if result is None:
        raise AssertionError(f"Milestone `{milestone_version}` doesn't exist")
    return result


def set_milestone(token: str, repo: str, issue_number: int, milestone_number: int) -> None:
    data = json.dumps({"milestone": milestone_number}).encode()
    headers = {"Authorization": f"token {token}",
               "Accept": "application/vnd.github.v3+json"}
    request = Request(f"https://api.github.com/repos/{repo}/issues/{issue_number}", data, headers, method="PATCH")
    urlopen(request)
