
package com.nihongolisten.admin;

import com.nihongolisten.common.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Moderation queue: duyệt/loại video crawler đưa về (yêu cầu role ADMIN — xem SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/videos")
public class AdminVideoController {

    private final JdbcTemplate jdbc;

    public AdminVideoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "PENDING_REVIEW") String status) {
        return jdbc.queryForList("""
                SELECT id, youtube_id AS "youtubeId", title, channel_name AS "channelName",
                       duration_sec AS "durationSec", jlpt_level AS "jlptLevel",
                       jlpt_confidence AS "jlptConfidence", status, crawled_at AS "crawledAt"
                FROM videos WHERE status = ? ORDER BY crawled_at DESC LIMIT 100
                """, status);
    }

    @PutMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id) {
        return updateStatus(id, "PUBLISHED");
    }

    @PutMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id) {
        return updateStatus(id, "REJECTED");
    }

    private Map<String, Object> updateStatus(long id, String status) {
        int updated = jdbc.update("UPDATE videos SET status = ? WHERE id = ?", status, id);
        if (updated == 0) {
            throw ApiException.notFound("Video không tồn tại");
        }
        return Map.of("id", id, "status", status);
    }
}
