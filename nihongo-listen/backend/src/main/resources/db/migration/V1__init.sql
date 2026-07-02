-- ===== Users =====
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(20)  NOT NULL DEFAULT 'FREE',   -- FREE | PREMIUM | ADMIN
    subscription_status VARCHAR(20)  NOT NULL DEFAULT 'NONE',   -- NONE | TRIAL | ACTIVE | CANCELED | EXPIRED
    subscription_end_at TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_profiles (
    user_id            BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name       VARCHAR(100),
    avatar_url         TEXT,
    jlpt_target        VARCHAR(2),      -- N5..N1
    daily_goal_minutes INT NOT NULL DEFAULT 15
);

-- ===== Videos & transcripts =====
CREATE TABLE videos (
    id              BIGSERIAL PRIMARY KEY,
    youtube_id      VARCHAR(20)  NOT NULL UNIQUE,
    title           TEXT         NOT NULL,
    channel_id      VARCHAR(50),
    channel_name    VARCHAR(255),
    duration_sec    INT,
    thumbnail_url   TEXT,
    published_at    TIMESTAMPTZ,
    view_count      BIGINT,
    jlpt_level      VARCHAR(2),          -- N5..N1
    jlpt_confidence REAL,
    wpm             REAL,
    has_furigana    BOOLEAN      NOT NULL DEFAULT FALSE,
    topic_tags      TEXT[]       NOT NULL DEFAULT '{}',
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING_REVIEW',  -- PENDING_REVIEW | PUBLISHED | REJECTED
    crawled_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_videos_status_level ON videos(status, jlpt_level);
CREATE INDEX idx_videos_published_at ON videos(published_at DESC);

CREATE TABLE transcripts (
    id       BIGSERIAL PRIMARY KEY,
    video_id BIGINT      NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL DEFAULT 'ja',
    source   VARCHAR(10) NOT NULL DEFAULT 'manual'   -- manual | auto
);
CREATE INDEX idx_transcripts_video ON transcripts(video_id);

CREATE TABLE transcript_segments (
    id            BIGSERIAL PRIMARY KEY,
    transcript_id BIGINT  NOT NULL REFERENCES transcripts(id) ON DELETE CASCADE,
    start_ms      INT     NOT NULL,
    end_ms        INT     NOT NULL,
    text          TEXT    NOT NULL,
    tokens_json   JSONB   -- [{surface, reading, base, pos, jlpt_level}, ...]
);
CREATE INDEX idx_segments_transcript ON transcript_segments(transcript_id, start_ms);

-- ===== Vocabulary & SRS =====
CREATE TABLE user_saved_words (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_base         VARCHAR(100) NOT NULL,
    reading           VARCHAR(100),
    meaning           TEXT,
    source_video_id   BIGINT REFERENCES videos(id) ON DELETE SET NULL,
    source_segment_id BIGINT REFERENCES transcript_segments(id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, word_base)
);

CREATE TABLE srs_reviews (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    word_id          BIGINT      NOT NULL REFERENCES user_saved_words(id) ON DELETE CASCADE,
    ease_factor      REAL        NOT NULL DEFAULT 2.5,
    interval_days    INT         NOT NULL DEFAULT 0,
    next_review_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_reviewed_at TIMESTAMPTZ,
    correct_count    INT         NOT NULL DEFAULT 0,
    wrong_count      INT         NOT NULL DEFAULT 0,
    UNIQUE (user_id, word_id)
);
CREATE INDEX idx_srs_due ON srs_reviews(user_id, next_review_at);

-- ===== Progress & notes =====
CREATE TABLE user_video_progress (
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id        BIGINT      NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    watched_seconds INT         NOT NULL DEFAULT 0,
    completed       BOOLEAN     NOT NULL DEFAULT FALSE,
    last_position   INT         NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, video_id)
);

CREATE TABLE user_notes (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    video_id     BIGINT      NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    timestamp_ms INT         NOT NULL,
    content      TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notes_user_video ON user_notes(user_id, video_id);

-- ===== Crawler =====
CREATE TABLE seed_keywords (
    id       BIGSERIAL PRIMARY KEY,
    keyword  VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(50),
    priority INT          NOT NULL DEFAULT 0,
    active   BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE channel_whitelist (
    id                 BIGSERIAL PRIMARY KEY,
    youtube_channel_id VARCHAR(50)  NOT NULL UNIQUE,
    channel_name       VARCHAR(255),
    avg_jlpt_level     VARCHAR(2),
    active             BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE crawl_jobs (
    id           BIGSERIAL PRIMARY KEY,
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at  TIMESTAMPTZ,
    videos_found INT         NOT NULL DEFAULT 0,
    videos_added INT         NOT NULL DEFAULT 0,
    quota_used   INT         NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'RUNNING'  -- RUNNING | DONE | FAILED
);

-- ===== Billing =====
CREATE TABLE subscriptions (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan                   VARCHAR(20) NOT NULL,   -- MONTHLY | YEARLY
    status                 VARCHAR(20) NOT NULL,   -- TRIAL | ACTIVE | CANCELED | EXPIRED
    stripe_subscription_id VARCHAR(255),
    current_period_end     TIMESTAMPTZ
);

CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount         BIGINT       NOT NULL,
    currency       VARCHAR(10)  NOT NULL DEFAULT 'VND',
    gateway        VARCHAR(20)  NOT NULL DEFAULT 'stripe',
    transaction_id VARCHAR(255),
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ===== Dictionary (seed từ JMdict) =====
CREATE TABLE dictionary_entries (
    id             BIGSERIAL PRIMARY KEY,
    kanji          VARCHAR(100),
    kana           VARCHAR(100) NOT NULL,
    meanings_json  JSONB        NOT NULL,
    jlpt_level     VARCHAR(2),
    frequency_rank INT
);
CREATE INDEX idx_dict_kanji ON dictionary_entries(kanji);
CREATE INDEX idx_dict_kana ON dictionary_entries(kana);
