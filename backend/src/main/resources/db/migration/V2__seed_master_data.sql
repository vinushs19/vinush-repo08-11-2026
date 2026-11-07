-- ==============================================================================
-- V2__seed_master_data.sql
-- AI-Based College Resource Finder — Initial Seed Data
-- Populates departments, categories, and a default admin account.
-- ==============================================================================

-- ---- Departments ----
INSERT INTO departments (department_code, department_name) VALUES
    ('MCA',   'Department of Computer Applications (PG)'),
    ('MBA',   'Department of Business Administration (PG)'),
    ('CSE',   'Department of Computer Science and Engineering'),
    ('ECE',   'Department of Electronics and Communication Engineering'),
    ('MATHS', 'Department of Mathematics'),
    ('ENG',   'Department of English');

-- ---- Resource Categories ----
INSERT INTO categories (category_name, description) VALUES
    ('Notes',           'Lecture notes and study materials'),
    ('Assignments',     'Assignments, lab exercises, and practical worksheets'),
    ('Question Papers', 'Previous semester examination question papers'),
    ('E-Books',         'Electronic books and reference manuals'),
    ('Lab Manuals',     'Laboratory manuals and procedure guides'),
    ('Syllabus',        'Semester syllabus and curriculum documents'),
    ('Others',          'Miscellaneous academic resources');

-- ---- Default Admin Account ----
-- Password: Admin@123 (BCrypt hash with 12 rounds)
-- IMPORTANT: Change this password immediately after first login.
INSERT INTO users (name, email, password, role, department_id, is_active) VALUES
    ('System Administrator',
     'admin@college.edu',
     '$2a$12$LJ3m4ys2JVyO5RqIst1b1OPUGnIxC3txgCF2VzkKXeBdGjMfpJzXi',
     'ADMIN',
     NULL,
     TRUE);
