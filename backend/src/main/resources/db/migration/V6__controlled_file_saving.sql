-- ==============================================================================
-- V6__controlled_file_saving.sql
-- AI-Based College Resource Finder — Controlled File-Saving Subsystem
-- Creates file_operations table with constraints, indexes, and audit metadata.
-- ==============================================================================

CREATE TABLE file_operations (
    operation_id         BIGINT        NOT NULL AUTO_INCREMENT,
    resource_id          BIGINT        NOT NULL,
    user_id              BIGINT        NOT NULL,
    source_file_path     VARCHAR(1000) NOT NULL,
    destination_category VARCHAR(50)   NOT NULL,
    destination_path     VARCHAR(1000) NOT NULL,
    destination_filename VARCHAR(500)  NOT NULL,
    file_size            BIGINT        NOT NULL DEFAULT 0,
    file_type            VARCHAR(50)   NOT NULL,
    operation_type       VARCHAR(50)   NOT NULL DEFAULT 'COPY_ORGANIZE',
    status               VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS',
    error_message        VARCHAR(500)  NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (operation_id),
    CONSTRAINT chk_file_ops_status CHECK (status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')),
    CONSTRAINT fk_file_ops_resource FOREIGN KEY (resource_id)
        REFERENCES resources (resource_id) ON DELETE CASCADE,
    CONSTRAINT fk_file_ops_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_file_ops_user ON file_operations (user_id);
CREATE INDEX idx_file_ops_resource ON file_operations (resource_id);
CREATE INDEX idx_file_ops_category ON file_operations (destination_category);
CREATE INDEX idx_file_ops_created ON file_operations (created_at DESC);
