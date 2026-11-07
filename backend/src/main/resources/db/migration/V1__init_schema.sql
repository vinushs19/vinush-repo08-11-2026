-- ==============================================================================
-- V1__init_schema.sql
-- AI-Based College Resource Finder — Initial Database Schema
-- Creates all core tables with constraints, indexes, and foreign keys.
-- ==============================================================================

-- ---- Departments ----
CREATE TABLE departments (
    department_id   BIGINT          NOT NULL AUTO_INCREMENT,
    department_code VARCHAR(20)     NOT NULL,
    department_name VARCHAR(150)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (department_id),
    CONSTRAINT uk_departments_code UNIQUE (department_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---- Categories ----
CREATE TABLE categories (
    category_id   BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    description   VARCHAR(500) NULL,
    PRIMARY KEY (category_id),
    CONSTRAINT uk_categories_name UNIQUE (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---- Users ----
CREATE TABLE users (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    department_id BIGINT       NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'FACULTY', 'ADMIN')),
    CONSTRAINT fk_users_department FOREIGN KEY (department_id)
        REFERENCES departments (department_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_department ON users (department_id);

-- ---- Subjects ----
CREATE TABLE subjects (
    subject_id    BIGINT       NOT NULL AUTO_INCREMENT,
    subject_code  VARCHAR(30)  NOT NULL,
    subject_name  VARCHAR(200) NOT NULL,
    semester      INT          NOT NULL,
    department_id BIGINT       NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subject_id),
    CONSTRAINT uk_subjects_code UNIQUE (subject_code),
    CONSTRAINT chk_subjects_semester CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT fk_subjects_department FOREIGN KEY (department_id)
        REFERENCES departments (department_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_subjects_dept_sem ON subjects (department_id, semester);

-- ---- Resources ----
CREATE TABLE resources (
    resource_id   BIGINT        NOT NULL AUTO_INCREMENT,
    title         VARCHAR(300)  NOT NULL,
    description   TEXT          NULL,
    file_path     VARCHAR(1000) NOT NULL,
    file_name     VARCHAR(500)  NOT NULL,
    file_type     VARCHAR(50)   NOT NULL,
    file_size     BIGINT        NOT NULL DEFAULT 0,
    resource_type VARCHAR(100)  NULL,
    subject_id    BIGINT        NULL,
    category_id   BIGINT        NULL,
    uploaded_by   BIGINT        NULL,
    summary       TEXT          NULL,
    saved_via_ai  BOOLEAN       NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    upload_date   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (resource_id),
    CONSTRAINT chk_resources_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT fk_resources_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id) ON DELETE SET NULL,
    CONSTRAINT fk_resources_category FOREIGN KEY (category_id)
        REFERENCES categories (category_id) ON DELETE SET NULL,
    CONSTRAINT fk_resources_uploader FOREIGN KEY (uploaded_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resources_subject ON resources (subject_id);
CREATE INDEX idx_resources_category ON resources (category_id);
CREATE INDEX idx_resources_filter ON resources (subject_id, category_id, status);
CREATE INDEX idx_resources_uploader ON resources (uploaded_by);
CREATE FULLTEXT INDEX idx_resources_search ON resources (title, description);

-- ---- Search History ----
CREATE TABLE search_history (
    search_id    BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    search_query VARCHAR(500) NOT NULL,
    result_count INT          NOT NULL DEFAULT 0,
    search_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (search_id),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_history_user_date ON search_history (user_id, search_date DESC);

-- ---- Students (Attendance Subsystem) ----
CREATE TABLE students (
    student_id      BIGINT       NOT NULL AUTO_INCREMENT,
    register_number VARCHAR(30)  NOT NULL,
    roll_number     VARCHAR(30)  NULL,
    name            VARCHAR(150) NOT NULL,
    department_id   BIGINT       NOT NULL,
    current_semester INT         NOT NULL DEFAULT 1,
    academic_batch  VARCHAR(20)  NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id),
    CONSTRAINT uk_students_register UNIQUE (register_number),
    CONSTRAINT chk_students_semester CHECK (current_semester BETWEEN 1 AND 8),
    CONSTRAINT fk_students_department FOREIGN KEY (department_id)
        REFERENCES departments (department_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_students_dept_sem ON students (department_id, current_semester);

-- ---- Attendance ----
CREATE TABLE attendance (
    attendance_id  BIGINT      NOT NULL AUTO_INCREMENT,
    student_id     BIGINT      NOT NULL,
    subject_id     BIGINT      NOT NULL,
    attendance_date DATE       NOT NULL,
    session_period INT         NOT NULL DEFAULT 1,
    status         VARCHAR(10) NOT NULL DEFAULT 'PRESENT',
    remarks        VARCHAR(255) NULL,
    marked_by      BIGINT      NULL,
    recorded_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attendance_id),
    CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENT', 'ABSENT')),
    CONSTRAINT uk_attendance_session UNIQUE (student_id, subject_id, attendance_date, session_period),
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id)
        REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attendance_marker FOREIGN KEY (marked_by)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_attendance_lookup ON attendance (student_id, subject_id, attendance_date);
CREATE INDEX idx_attendance_date ON attendance (attendance_date);

-- ---- Audit Logs ----
CREATE TABLE audit_logs (
    log_id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NULL,
    action              VARCHAR(100) NOT NULL,
    resource_type       VARCHAR(100) NULL,
    resource_identifier VARCHAR(255) NULL,
    ip_address          VARCHAR(45)  NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    details             TEXT         NULL,
    timestamp           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_user ON audit_logs (user_id);
CREATE INDEX idx_audit_action ON audit_logs (action, timestamp);
CREATE INDEX idx_audit_timestamp ON audit_logs (timestamp DESC);
