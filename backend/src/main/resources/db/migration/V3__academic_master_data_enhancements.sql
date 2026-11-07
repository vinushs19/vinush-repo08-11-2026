-- ==============================================================================
-- V3__academic_master_data_enhancements.sql
-- AI-Based College Resource Finder — Academic Master Data Enhancements
-- Adds semesters table, units table, active-status tracking, and seed data.
-- ==============================================================================

-- 1. Departments enhancements (soft-deactivation and update tracking)
ALTER TABLE departments
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP;

-- 2. Categories enhancements
ALTER TABLE categories
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP;

-- 3. Subjects enhancements
ALTER TABLE subjects
    ADD COLUMN description TEXT NULL,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP;

-- 4. Semesters table
CREATE TABLE semesters (
    semester_id     BIGINT       NOT NULL AUTO_INCREMENT,
    semester_number INT          NOT NULL,
    semester_name   VARCHAR(100) NOT NULL,
    term            VARCHAR(20)  NOT NULL DEFAULT 'ODD',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (semester_id),
    CONSTRAINT uk_semesters_number UNIQUE (semester_number),
    CONSTRAINT chk_semesters_number CHECK (semester_number BETWEEN 1 AND 8),
    CONSTRAINT chk_semesters_term CHECK (term IN ('ODD', 'EVEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Units table (Course curriculum syllabus units 1-10 per subject)
CREATE TABLE units (
    unit_id       BIGINT       NOT NULL AUTO_INCREMENT,
    subject_id    BIGINT       NOT NULL,
    unit_number   INT          NOT NULL,
    unit_title    VARCHAR(200) NOT NULL,
    description   TEXT         NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (unit_id),
    CONSTRAINT uk_units_subject_number UNIQUE (subject_id, unit_number),
    CONSTRAINT chk_units_number CHECK (unit_number BETWEEN 1 AND 10),
    CONSTRAINT fk_units_subject FOREIGN KEY (subject_id)
        REFERENCES subjects (subject_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_units_subject ON units (subject_id);

-- 6. Seed Semesters (1 to 8)
INSERT INTO semesters (semester_number, semester_name, term) VALUES
    (1, 'First Semester',   'ODD'),
    (2, 'Second Semester',  'EVEN'),
    (3, 'Third Semester',   'ODD'),
    (4, 'Fourth Semester',  'EVEN'),
    (5, 'Fifth Semester',   'ODD'),
    (6, 'Sixth Semester',   'EVEN'),
    (7, 'Seventh Semester', 'ODD'),
    (8, 'Eighth Semester',  'EVEN');

-- 7. Seed Sample MCA Subjects
INSERT INTO subjects (subject_code, subject_name, semester, department_id, description)
SELECT '26MCA101', 'Data Structures and Algorithms', 1, department_id, 'Foundational computing and algorithm analysis'
FROM departments WHERE department_code = 'MCA' LIMIT 1;

INSERT INTO subjects (subject_code, subject_name, semester, department_id, description)
SELECT '26MCA201', 'Database Management Systems', 2, department_id, 'Relational modeling, SQL, normalization and transactions'
FROM departments WHERE department_code = 'MCA' LIMIT 1;

INSERT INTO subjects (subject_code, subject_name, semester, department_id, description)
SELECT '26MCA301', 'Java Enterprise Applications', 3, department_id, 'Spring framework, enterprise design patterns and REST services'
FROM departments WHERE department_code = 'MCA' LIMIT 1;

-- 8. Seed Sample Units for DBMS
INSERT INTO units (subject_id, unit_number, unit_title, description)
SELECT s.subject_id, 1, 'Unit 1: Introduction to DBMS and Data Models', 'Evolution of database technology, three-schema architecture, and relational algebra'
FROM subjects s WHERE s.subject_code = '26MCA201' LIMIT 1;

INSERT INTO units (subject_id, unit_number, unit_title, description)
SELECT s.subject_id, 2, 'Unit 2: SQL and Relational Integrity', 'DDL, DML, advanced queries, integrity constraints and views'
FROM subjects s WHERE s.subject_code = '26MCA201' LIMIT 1;

INSERT INTO units (subject_id, unit_number, unit_title, description)
SELECT s.subject_id, 3, 'Unit 3: Database Normalization', 'Functional dependencies, 1NF, 2NF, 3NF, BCNF and decomposition algorithms'
FROM subjects s WHERE s.subject_code = '26MCA201' LIMIT 1;

INSERT INTO units (subject_id, unit_number, unit_title, description)
SELECT s.subject_id, 4, 'Unit 4: Transaction Processing & Concurrency', 'ACID properties, serializability, two-phase locking, and deadlock resolution'
FROM subjects s WHERE s.subject_code = '26MCA201' LIMIT 1;

INSERT INTO units (subject_id, unit_number, unit_title, description)
SELECT s.subject_id, 5, 'Unit 5: Storage and Query Optimization', 'Indexing, B+ Trees, hashing, and relational query cost estimation'
FROM subjects s WHERE s.subject_code = '26MCA201' LIMIT 1;
