package com.nihongolisten.video;

import com.nihongolisten.common.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Public video API (GUEST xem được — quota preview xử lý sau ở W11-12).
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final String VIDEO_COLUMNS = """
            id, youtube_id AS "youtubeId", title, channel_name AS "channelName",
            duration_sec AS "durationSec", thumbnail_url AS "thumbnailUrl",
            jlpt_level AS "jlptLevel", jlpt_confidence AS "jlptConfidence",
            wpm, array_to_string(topic_tags, ',') AS "topicTags", published_at AS "publishedAt"
            """;

    private final JdbcTemplate jdbc;

    public VideoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String level,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 0) * safeSize;

        List<Map<String, Object>> content;
        long total;
        if (level != null && !level.isBlank()) {
            content = jdbc.queryForList(
                    "SELECT " + VIDEO_COLUMNS + " FROM videos WHERE status = 'PUBLISHED' AND jlpt_level = ? " +
                            "ORDER BY crawled_at DESC LIMIT ? OFFSET ?",
                    level, safeSize, offset);
            total = jdbc.queryForObject(
                    "SELECT count(*) FROM videos WHERE status = 'PUBLISHED' AND jlpt_level = ?",
                    Long.class, level);
        } else {
            content = jdbc.queryForList(
                    "SELECT " + VIDEO_COLUMNS + " FROM videos WHERE status = 'PUBLISHED' " +
                            "ORDER BY crawled_at DESC LIMIT ? OFFSET ?",
                    safeSize, offset);
            total = jdbc.queryForObject(
                    "SELECT count(*) FROM videos WHERE status = 'PUBLISHED'", Long.class);
        }

        return Map.of("content", content, "page", page, "size", safeSize, "totalElements", total);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return jdbc.queryForList(
                        "SELECT " + VIDEO_COLUMNS + " FROM videos WHERE id = ? AND status = 'PUBLISHED'", id)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Video không tồn tại"));
    }

    @GetMapping("/{id}/transcript")
    public Map<String, Object> transcript(@PathVariable long id) {
        List<Map<String, Object>> transcripts = jdbc.queryForList(
                "SELECT id, language, source FROM transcripts WHERE video_id = ? ORDER BY id DESC LIMIT 1", id);
        if (transcripts.isEmpty()) {
            throw ApiException.notFound("Video chưa có transcript");
        }
        Map<String, Object> transcript = transcripts.get(0);
        List<Map<String, Object>> segments = jdbc.queryForList("""
                SELECT id, start_ms AS "startMs", end_ms AS "endMs", text, tokens_json::text AS "tokensJson"
                FROM transcript_segments WHERE transcript_id = ? ORDER BY start_ms
                """, transcript.get("id"));
        return Map.of(
                "transcriptId", transcript.get("id"),
                "language", transcript.get("language"),
                "source", transcript.get("source"),
                "segments", segments);
    }
}
