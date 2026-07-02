"""Metadata video qua yt-dlp (không tải video — chỉ extract info)."""
import logging

import yt_dlp

logger = logging.getLogger(__name__)

_YDL_OPTS = {
    "quiet": True,
    "no_warnings": True,
    "skip_download": True,
    "extract_flat": False,
}


def search_videos(keyword: str, limit: int = 10) -> list[dict]:
    """Tìm video YouTube theo từ khóa bằng yt-dlp (không cần API key).

    Trả về list [{id, title, durationSec}] — extract_flat nên nhanh,
    metadata đầy đủ sẽ lấy sau khi video qua được filter transcript.
    """
    opts = {**_YDL_OPTS, "extract_flat": True}
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(f"ytsearch{limit}:{keyword}", download=False)
        results = []
        for entry in info.get("entries") or []:
            if entry and entry.get("id"):
                results.append({
                    "id": entry["id"],
                    "title": entry.get("title"),
                    "durationSec": int(entry["duration"]) if entry.get("duration") else None,
                })
        return results
    except Exception:
        logger.exception("yt-dlp search thất bại cho từ khóa '%s'", keyword)
        return []


def fetch_video_metadata(youtube_id: str) -> dict:
    """Trả về metadata cơ bản; các field có thể None nếu extract thất bại."""
    url = f"https://www.youtube.com/watch?v={youtube_id}"
    try:
        with yt_dlp.YoutubeDL(_YDL_OPTS) as ydl:
            info = ydl.extract_info(url, download=False)
        return {
            "title": info.get("title"),
            "channelId": info.get("channel_id"),
            "channelName": info.get("channel") or info.get("uploader"),
            "durationSec": info.get("duration"),
            "thumbnailUrl": info.get("thumbnail"),
            "viewCount": info.get("view_count"),
        }
    except Exception:
        logger.exception("yt-dlp không lấy được metadata cho %s", youtube_id)
        return {
            "title": None,
            "channelId": None,
            "channelName": None,
            "durationSec": None,
            "thumbnailUrl": None,
            "viewCount": None,
        }
