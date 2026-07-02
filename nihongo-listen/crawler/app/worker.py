"""Worker đọc Redis queue "transcript-fetch" (W9-10: CrawlOrchestrator đẩy job vào đây).

Chạy: python -m app.worker
Hiện tại là skeleton — xử lý tuần tự từng youtube_id trong queue.
"""
import json
import logging

import redis

from app.config import settings
from app.main import FetchTranscriptRequest, fetch_transcript

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

QUEUE_NAME = "transcript-fetch"


def run() -> None:
    client = redis.from_url(settings.redis_url, decode_responses=True)
    logger.info("Worker đang lắng nghe queue '%s'...", QUEUE_NAME)
    while True:
        _, raw = client.blpop(QUEUE_NAME)
        try:
            job = json.loads(raw)
            youtube_id = job["youtube_id"]
        except (json.JSONDecodeError, KeyError):
            logger.error("Job không hợp lệ, bỏ qua: %s", raw)
            continue

        try:
            result = fetch_transcript(FetchTranscriptRequest(youtube_id=youtube_id))
            logger.info("Xong %s: %s segments", youtube_id, result["segmentCount"])
        except Exception:
            logger.exception("Xử lý %s thất bại", youtube_id)


if __name__ == "__main__":
    run()
