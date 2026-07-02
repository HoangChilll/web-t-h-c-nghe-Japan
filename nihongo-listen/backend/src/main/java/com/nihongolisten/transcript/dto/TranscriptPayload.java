package com.nihongolisten.transcript.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Payload crawler POST về /internal/transcripts (JSON camelCase).
 */
public record TranscriptPayload(
        @NotBlank String youtubeId,
        @NotBlank String title,
        String channelId,
        String channelName,
        Integer durationSec,
        String thumbnailUrl,
        Long viewCount,
        String language,
        String source,          // 'manual' | 'auto'
        @NotEmpty @Valid List<Segment> segments
) {
    public record Segment(
            int startMs,
            int endMs,
            @NotBlank String text
    ) {
    }
}
