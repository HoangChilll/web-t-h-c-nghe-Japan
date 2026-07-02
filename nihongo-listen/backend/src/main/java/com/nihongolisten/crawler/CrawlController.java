package com.nihongolisten.crawler;

import com.nihongolisten.common.exception.ApiException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin/crawl")
public class CrawlController {

    private static final Pattern VIDEO_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern URL_ID = Pattern.compile(
            "(?:youtu\\.be/|v=|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})");

    private final CrawlerClient crawlerClient;

    public CrawlController(CrawlerClient crawlerClient) {
        this.crawlerClient = crawlerClient;
    }

    public record TriggerRequest(String youtubeId, String url) {
    }

    public record SearchRequest(String keyword, Integer maxResults) {
    }

    /**
     * Crawl thủ công 1 video — nhận video ID hoặc URL YouTube đầy đủ.
     * TODO W9-10: CrawlOrchestrator + Quartz cron 6h + Redis queue cho batch crawl.
     */
    @PostMapping("/trigger")
    public Map<String, Object> trigger(@RequestBody TriggerRequest request) {
        String input = request.youtubeId() != null && !request.youtubeId().isBlank()
                ? request.youtubeId().trim()
                : request.url() != null ? request.url().trim() : "";
        if (input.isEmpty()) {
            throw ApiException.badRequest("Cần youtubeId hoặc url");
        }
        return crawlerClient.triggerFetchTranscript(extractVideoId(input));
    }

    /** Tìm video theo từ khóa rồi crawl các video có transcript tiếng Nhật. */
    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody SearchRequest request) {
        if (request.keyword() == null || request.keyword().isBlank()) {
            throw ApiException.badRequest("Cần keyword");
        }
        int maxResults = request.maxResults() != null ? request.maxResults() : 3;
        if (maxResults < 1 || maxResults > 10) {
            throw ApiException.badRequest("maxResults phải trong khoảng 1-10");
        }
        return crawlerClient.triggerSearchCrawl(request.keyword().trim(), maxResults);
    }

    /** Chấp nhận video ID trần hoặc mọi dạng URL YouTube phổ biến (watch, youtu.be, shorts, embed, live). */
    static String extractVideoId(String input) {
        if (VIDEO_ID.matcher(input).matches()) {
            return input;
        }
        Matcher m = URL_ID.matcher(input);
        if (m.find()) {
            return m.group(1);
        }
        throw ApiException.badRequest("Không nhận diện được video ID từ: " + input);
    }
}
