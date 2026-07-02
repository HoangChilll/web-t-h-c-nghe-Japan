package com.nihongolisten.transcript;

import com.nihongolisten.transcript.dto.TranscriptPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Nhận transcript từ crawler và lưu videos + transcripts + transcript_segments.
 * TODO W7-8: Kuromoji tokenize từng segment → tokens_json + JLPT scoring cho video.
 */
@Service
public class TranscriptIngestService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptIngestService.class);

    private final JdbcTemplate jdbc;

    public TranscriptIngestService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Map<String, Object> ingest(TranscriptPayload payload) {
        // Upsert video theo youtube_id, status PENDING_REVIEW chờ admin duyệt
        Long videoId = jdbc.queryForObject("""
                INSERT INTO videos (youtube_id, title, channel_id, channel_name, duration_sec,
                                    thumbnail_url, view_count, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING_REVIEW')
                ON CONFLICT (youtube_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    channel_id = EXCLUDED.channel_id,
                    channel_name = EXCLUDED.channel_name,
                    duration_sec = EXCLUDED.duration_sec,
                    thumbnail_url = EXCLUDED.thumbnail_url,
                    view_count = EXCLUDED.view_count,
                    crawled_at = now()
                RETURNING id
                """, Long.class,
                payload.youtubeId(), payload.title(), payload.channelId(), payload.channelName(),
                payload.durationSec(), payload.thumbnailUrl(), payload.viewCount());

        String language = payload.language() != null ? payload.language() : "ja";
        String source = payload.source() != null ? payload.source() : "manual";

        // Crawl lại → thay transcript cũ cùng ngôn ngữ (segments xoá theo CASCADE)
        jdbc.update("DELETE FROM transcripts WHERE video_id = ? AND language = ?", videoId, language);
        Long transcriptId = jdbc.queryForObject(
                "INSERT INTO transcripts (video_id, language, source) VALUES (?, ?, ?) RETURNING id",
                Long.class, videoId, language, source);

        jdbc.batchUpdate(
                "INSERT INTO transcript_segments (transcript_id, start_ms, end_ms, text) VALUES (?, ?, ?, ?)",
                payload.segments(), 500,
                (ps, seg) -> {
                    ps.setLong(1, transcriptId);
                    ps.setInt(2, seg.startMs());
                    ps.setInt(3, seg.endMs());
                    ps.setString(4, seg.text());
                });

        log.info("Ingested transcript: youtubeId={}, videoId={}, segments={}",
                payload.youtubeId(), videoId, payload.segments().size());

        return Map.of(
                "videoId", videoId,
                "transcriptId", transcriptId,
                "segmentCount", payload.segments().size(),
                "status", "PENDING_REVIEW");
    }
}
