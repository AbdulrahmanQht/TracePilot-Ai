from __future__ import annotations

import re

_JSON_FENCE_RE = re.compile(r"^```(?:json)?\s*|\s*```$", re.MULTILINE)


def strip_json_fence(text: str) -> str:
    """Some models wrap JSON in ```json fences despite being told not to."""
    return _JSON_FENCE_RE.sub("", text.strip()).strip()
