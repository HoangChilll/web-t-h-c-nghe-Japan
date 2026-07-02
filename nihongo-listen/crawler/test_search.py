# Script test nhanh search-crawl (chạy: .venv\Scripts\python test_search.py "từ khóa")
import json
import sys

import httpx

keyword = sys.argv[1] if len(sys.argv) > 1 else "日本語 リスニング N5"
login = httpx.post(
    "http://localhost:8080/api/auth/login",
    json={"email": "test@example.com", "password": "password123"},
).json()
resp = httpx.post(
    "http://localhost:8080/api/admin/crawl/search",
    json={"keyword": keyword, "maxResults": 2},
    headers={"Authorization": f"Bearer {login['accessToken']}"},
    timeout=600,
)
d = resp.json()
out = {
    "status": resp.status_code,
    "candidatesFound": d.get("candidatesFound"),
    "ingestedCount": d.get("ingestedCount"),
    "ingested": [
        {"id": v["youtubeId"], "title": v["title"][:60], "segments": v["segmentCount"]}
        for v in d.get("ingested", [])
    ],
    "skipped": [s["reason"][:80] for s in d.get("skipped", [])][:6],
}
print(json.dumps(out, ensure_ascii=True, indent=1))
