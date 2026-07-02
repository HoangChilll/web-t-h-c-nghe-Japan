"""Nihongo Listen — Crawler service (FastAPI).

Endpoints (xác thực bằng header X-Internal-Secret):
  POST /fetch-transcript  — crawl 1 video theo youtube_id
  POST /search-crawl      — tìm video theo từ khóa (yt-dlp search, không cần API key)
                            rồi crawl những video có transcript tiếng Nhật

Mỗi video crawl xong được POST về backend /internal/transcripts với cùng shared secret.
"""
import logging

import httpx
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

from app.config import settings
from app.transcript import TranscriptNotAvailable, fetch_japanese_transcript
from app.youtube import fetch_video_metadata, search_videos

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Nihongo Listen Crawler", version="0.2.0")

# Tiêu chí crawl theo spec: video 3–30 phút
MIN_DURATION_SEC = 3 * 60
MAX_DURATION_SEC = 30 * 60


def verify_internal_secret(x_internal_secret: str = Header(default="")) -> None:
    if x_internal_secret != settings.crawler_internal_secret:
        raise HTTPException(status_code=401, detail="invalid internal secret")


class FetchTranscriptRequest(BaseModel):
    youtube_id: str = Field(min_length=5, max_length=20)


class SearchCrawlRequest(BaseModel):
    keyword: str = Field(min_length=1, max_length=200)
    max_results: int = Field(default=3, ge=1, le=10)


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


def process_video(youtube_id: str) -> dict:
    """Crawl 1 video: transcript + metadata rồi đẩy về backend.

    Raises TranscriptNotAvailable nếu video không có transcript tiếng Nhật.
    Raises httpx.HTTPError nếu backend không nhận được.
    """
    transcript = fetch_japanese_transcript(youtube_id)
    metadata = fetch_video_metadata(youtube_id)

    payload = {
        "youtubeId": youtube_id,
        "title": metadata["title"] or f"YouTube video {youtube_id}",
        "channelId": metadata["channelId"],
        "channelName": metadata["channelName"],
        "durationSec": metadata["durationSec"],
        "thumbnailUrl": metadata["thumbnailUrl"],
        "viewCount": metadata["viewCount"],
        "language": transcript.language,
        "source": transcript.source,
        "segments": transcript.segments,
    }

    resp = httpx.post(
        f"{settings.backend_internal_url}/internal/transcripts",
        json=payload,
        headers={"X-Internal-Secret": settings.crawler_internal_secret},
        timeout=30,
    )
    resp.raise_for_status()

    result = resp.json()
    logger.info("Ingested %s: %s", youtube_id, result)
    return {
        "youtubeId": youtube_id,
        "title": payload["title"],
        "source": transcript.source,
        "segmentCount": len(transcript.segments),
        "backend": result,
    }


@app.post("/fetch-transcript", dependencies=[Depends(verify_internal_secret)])
def fetch_transcript(req: FetchTranscriptRequest) -> dict:
    youtube_id = req.youtube_id.strip()
    logger.info("Fetch transcript: %s", youtube_id)
    try:
        return process_video(youtube_id)
    except TranscriptNotAvailable as e:
        raise HTTPException(status_code=422, detail=str(e))
    except httpx.HTTPError as e:
        logger.error("Không gửi được transcript về backend: %s", e)
        raise HTTPException(status_code=502, detail=f"Backend ingest failed: {e}")


@app.post("/search-crawl", dependencies=[Depends(verify_internal_secret)])
def search_crawl(req: SearchCrawlRequest) -> dict:
    """Tìm video theo từ khóa rồi crawl những video đạt tiêu chí.

    Tìm dư gấp 3 lần max_results vì nhiều video không có transcript tiếng Nhật
    hoặc không đạt tiêu chí độ dài.
    """
    keyword = req.keyword.strip()
    logger.info("Search crawl: '%s' (max %d video)", keyword, req.max_results)

    candidates = search_videos(keyword, limit=req.max_results * 3)
    ingested: list[dict] = []
    skipped: list[dict] = []

    for candidate in candidates:
        if len(ingested) >= req.max_results:
            break
        youtube_id = candidate["id"]
        duration = candidate.get("durationSec")
        if duration and not (MIN_DURATION_SEC <= duration <= MAX_DURATION_SEC):
            skipped.append({"youtubeId": youtube_id, "reason": f"độ dài {duration}s ngoài khoảng 3-30 phút"})
            continue
        try:
            ingested.append(process_video(youtube_id))
        except TranscriptNotAvailable as e:
            skipped.append({"youtubeId": youtube_id, "reason": str(e)})
        except httpx.HTTPError as e:
            skipped.append({"youtubeId": youtube_id, "reason": f"backend ingest failed: {e}"})

    return {
        "keyword": keyword,
        "candidatesFound": len(candidates),
        "ingestedCount": len(ingested),
        "ingested": ingested,
        "skipped": skipped,
    }
