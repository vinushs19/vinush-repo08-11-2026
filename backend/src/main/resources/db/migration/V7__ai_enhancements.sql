-- ==============================================================================
-- AI-Based College Resource Finder — Database Migration V7
-- Schema enhancements for AI/NLP features: Document summaries, file hashes,
-- and conversational interaction audit tracking.
-- ==============================================================================

-- 1. Extend resources table with summary metadata and content hash
ALTER TABLE resources
    ADD COLUMN summary_updated_at TIMESTAMP NULL AFTER summary,
    ADD COLUMN file_hash VARCHAR(64) NULL AFTER summary_updated_at;

-- 2. Audit table for tracking AI Academic Assistant interactions
CREATE TABLE chat_interactions (
    interaction_id     BIGINT        NOT NULL AUTO_INCREMENT,
    user_id            BIGINT        NOT NULL,
    session_id         VARCHAR(100)  NOT NULL,
    user_message       TEXT          NOT NULL,
    detected_intent    VARCHAR(100)  NOT NULL,
    assistant_reply    TEXT          NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    latency_ms         INT           NOT NULL DEFAULT 0,
    timestamp          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (interaction_id),
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_user_session ON chat_interactions (user_id, session_id);
CREATE INDEX idx_chat_intent ON chat_interactions (detected_intent);
