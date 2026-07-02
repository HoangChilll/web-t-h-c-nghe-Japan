"""Lấy transcript tiếng Nhật từ YouTube bằng youtube-transcript-api.

Ưu tiên transcript manual (chất lượng cao, tiêu chí crawl của dự án),
fallback sang auto-generated nếu không có.
"""
from dataclasses import dataclass

from youtube_transcript_api import YouTubeTranscriptApi
from youtube_transcript_api._errors import NoTranscriptFound, TranscriptsDisabled

JA_LANGS = ["ja", "ja-JP"]


@dataclass
class TranscriptResult:
    source: str  # 'manual' | 'auto'
    language: str
    segments: list[dict]  # [{startMs, endMs, text}]


class TranscriptNotAvailable(Exception):
    pass


def fetch_japanese_transcript(youtube_id: str) -> TranscriptResult:
    api = YouTubeTranscriptApi()
    try:
        transcript_list = api.list(youtube_id)
    except TranscriptsDisabled as e:
        raise TranscriptNotAvailable(f"Video {youtube_id} đã tắt transcript") from e

    transcript = None
    source = "manual"
    try:
        transcript = transcript_list.find_manually_created_transcript(JA_LANGS)
    except NoTranscriptFound:
        try:
            transcript = transcript_list.find_generated_transcript(JA_LANGS)
            source = "auto"
        except NoTranscriptFound as e:
            raise TranscriptNotAvailable(
                f"Video {youtube_id} không có transcript tiếng Nhật"
            ) from e

    fetched = transcript.fetch()
    segments = []
    for snippet in fetched:
        start_ms = int(snippet.start * 1000)
        end_ms = int((snippet.start + snippet.duration) * 1000)
        text = snippet.text.strip()
        if text:
            segments.append({"startMs": start_ms, "endMs": end_ms, "text": text})

    if not segments:
        raise TranscriptNotAvailable(f"Transcript của video {youtube_id} rỗng")

    return TranscriptResult(source=source, language=transcript.language_code, segments=segments)
