package com.nihongolisten.crawler;

import com.nihongolisten.common.config.CrawlerProperties;
import com.nihongolisten.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP client gọi sang Python crawler service, xác thực bằng shared secret.
 */
@Component
public class CrawlerClient {

    private static final Logger log = LoggerFactory.getLogger(CrawlerClient.class);

    private final RestClient restClient;

    public CrawlerClient(CrawlerProperties props) {
        // Ép HTTP/1.1: JDK HttpClient mặc định gửi upgrade h2c mà uvicorn (h11) không hỗ trợ,
        // làm request body bị drop phía crawler.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        // Search-crawl xử lý nhiều video tuần tự nên có thể mất vài phút
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(props.baseUrl())
                .defaultHeader("X-Internal-Secret", props.internalSecret())
                .build();
    }

    /**
     * Yêu cầu crawler fetch transcript cho một video YouTube.
     * Crawler xử lý xong sẽ tự POST kết quả về /internal/transcripts.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> triggerFetchTranscript(String youtubeId) {
        return post("/fetch-transcript", Map.of("youtube_id", youtubeId));
    }

    /** Tìm video theo từ khóa (yt-dlp search) rồi crawl các video có transcript ja. */
    public Map<String, Object> triggerSearchCrawl(String keyword, int maxResults) {
        return post("/search-crawl", Map.of("keyword", keyword, "max_results", maxResults));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String uri, Map<String, ?> body) {
        try {
            return restClient.post()
                    .uri(uri)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.error("Không gọi được crawler service: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Crawler service không phản hồi: " + e.getMessage());
        }
    }
}
