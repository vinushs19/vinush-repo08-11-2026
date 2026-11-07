-- ==============================================================================
-- V4__resource_repository_enhancements.sql
-- AI-Based College Resource Finder — Academic Resource Repository Enhancements
-- Adds unit foreign key, original filename, active status tracking, and updated_at.
-- ==============================================================================

-- 1. Add unit mapping, original filename, active status, and modification timestamp
ALTER TABLE resources
    ADD COLUMN original_filename VARCHAR(500) NULL AFTER file_name,
    ADD COLUMN unit_id           BIGINT       NULL AFTER subject_id,
    ADD COLUMN is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN updated_at        TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP;

-- 2. Relax status check constraint to support soft-deletion (ACTIVE, ARCHIVED, DELETED)
ALTER TABLE resources
    DROP CHECK chk_resources_status;

ALTER TABLE resources
    ADD CONSTRAINT chk_resources_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'));

-- 3. Add foreign key from resources to units
ALTER TABLE resources
    ADD CONSTRAINT fk_resources_unit FOREIGN KEY (unit_id)
        REFERENCES units (unit_id) ON DELETE SET NULL;

-- 4. Create performance indexes for resource taxonomy filtering
CREATE INDEX idx_resources_unit ON resources (unit_id);
CREATE INDEX idx_resources_active ON resources (is_active);
CREATE INDEX idx_resources_upload_date ON resources (upload_date DESC);
CREATE INDEX idx_resources_type ON resources (resource_type);
