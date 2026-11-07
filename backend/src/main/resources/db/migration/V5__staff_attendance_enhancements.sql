-- ==============================================================================
-- V5__staff_attendance_enhancements.sql
-- AI-Based College Resource Finder — Staff Attendance Subsystem
-- Creates staff_attendance table with constraints, indexes, and origin tracking.
-- ==============================================================================

CREATE TABLE staff_attendance (
    staff_attendance_id BIGINT       NOT NULL AUTO_INCREMENT,
    staff_identifier    VARCHAR(50)  NOT NULL,
    staff_name          VARCHAR(150) NOT NULL,
    department_id       BIGINT       NOT NULL,
    user_id             BIGINT       NULL,
    attendance_date     DATE         NOT NULL,
    session_period      INT          NOT NULL DEFAULT 1,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PRESENT',
    data_source         VARCHAR(50)  NOT NULL DEFAULT 'MANUAL_ENTRY',
    remarks             VARCHAR(255) NULL,
    recorded_by         BIGINT       NULL,
    recorded_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NULL,
    PRIMARY KEY (staff_attendance_id),
    CONSTRAINT chk_staff_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'ON_DUTY', 'LEAVE')),
    CONSTRAINT chk_staff_session_period CHECK (session_period BETWEEN 1 AND 8),
    CONSTRAINT uk_staff_attendance_session UNIQUE (staff_identifier, attendance_date, session_period),
    CONSTRAINT fk_staff_attendance_dept FOREIGN KEY (department_id)
        REFERENCES departments (department_id) ON DELETE RESTRICT,
    CONSTRAINT fk_staff_attendance_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE SET NULL,
    CONSTRAINT fk_staff_attendance_marker FOREIGN KEY (recorded_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_staff_att_search ON staff_attendance (staff_identifier, attendance_date);
CREATE INDEX idx_staff_att_dept_date ON staff_attendance (department_id, attendance_date);
CREATE INDEX idx_staff_att_date ON staff_attendance (attendance_date);
