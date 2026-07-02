package com.nihongolisten.transcript;

import com.nihongolisten.transcript.dto.TranscriptPayload;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint nội bộ cho crawler service — được bảo vệ bởi InternalSecretFilter
 * (header X-Internal-Secret), không dùng JWT.
 */
@RestController
@RequestMapping("/internal")
public class InternalTranscriptController {

    private final TranscriptIngestService ingestService;

    public InternalTranscriptController(TranscriptIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/transcripts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> receiveTranscript(@Valid @RequestBody TranscriptPayload payload) {
        return ingestService.ingest(payload);
    }
}
