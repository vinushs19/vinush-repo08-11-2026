.
.
.
# AI-Based College Resource Finder

> **MCA Mini-Project** &mdash; Hindusthan College of Arts &amp; Science (Autonomous), Coimbatore  
> **Affiliated to:** Bharathiar University, Coimbatore  
> **Author:** Vinush S (Reg No: 26MCA091)  
> **Supervisor:** Mrs. V. Latha Sivasankari, Assistant Professor, Dept. of Computer Applications (PG)

---

## 1. Project Overview

The **AI-Based College Resource Finder** is a centralized web platform designed to streamline the discovery, retrieval, and management of higher education academic resources (lecture notes, question banks, assignments, e-books, and lab manuals) across college departments. The platform integrates academic master data, secure local file storage, and subject-wise attendance tracking, engineered with a core-first architecture that cleanly prepares for future AI/NLP conversational capabilities.

---

## 2. Repository Structure

```
ai_basced_college_resource_finder/
├── .env.example              # Environment variables template (placeholders only)
├── .gitignore                # Git ignore rules for build artifacts, storage, and secrets
├── README.md                 # Project documentation and developer guide
├── docs/                     # System design, architecture diagrams, and requirements
│   └── architecture_spec.md  # Approved Module 00 Architecture Specification
├── frontend/                 # Client tier (Bootstrap 5, Vanilla ES6 JavaScript, HTML5, CSS3)
│   ├── index.html            # Landing page and environment health status monitor
│   ├── css/
│   │   └── style.css         # Custom responsive stylesheet
│   └── js/
│       └── app.js            # Frontend health-check and diagnostics logic
├── backend/                  # Server tier (Java 21, Spring Boot 3.x, Maven)
│   ├── pom.xml               # Maven configuration (Web, JPA, MySQL, Flyway, Validation, Actuator)
│   └── src/
│       ├── main/
│       │   ├── java/com/college/resourcefinder/
│       │   │   ├── ResourceFinderApplication.java  # Main application entry point
│       │   │   ├── config/                         # StorageProperties, WebMvcConfig
│       │   │   ├── controller/                     # HealthController (/api/v1/health)
│       │   │   ├── dto/response/                   # ApiResponse<T>, ErrorResponse
│       │   │   ├── entity/                         # JPA entities (Department, Subject, User, etc.)
│       │   │   ├── exception/                      # GlobalExceptionHandler, Custom Exceptions
│       │   │   ├── repository/                     # Spring Data JPA repositories
│       │   │   └── util/                           # FileValidator, SecurityUtils
│       │   └── resources/
│       │       ├── application.yml                 # Environment-based configuration
│       │       ├── db/migration/                   # Flyway database migrations (V1, V2)
│       │       └── static/                         # Mirrored frontend assets for embedded runtime
│       └── test/
│           ├── java/com/college/resourcefinder/    # JUnit 5 & MockMvc test suite
│           └── resources/
│               └── application-test.yml            # In-memory H2 configuration for tests
├── storage/                  # Controlled local storage directory for uploaded academic assets
│   └── .gitkeep              # Preserves directory structure in Git
└── scripts/                  # Windows automation and database setup scripts
    ├── setup-database.sql    # MySQL database creation script
    ├── start-backend.bat     # Windows Batch startup script
    ├── start-backend.ps1     # Windows PowerShell startup script
    └── build.bat             # Build and test verification script
```

---

## 3. Technology Stack

- **Runtime & Language:** Java 21 LTS (Oracle / OpenJDK)
- **Framework:** Spring Boot 3.4.1
  - Spring Web (REST APIs)
  - Spring Data JPA & Hibernate ORM
  - Bean Validation (`jakarta.validation`)
  - Spring Boot Actuator (Metrics & diagnostics)
- **Build Tool:** Apache Maven 3.9+
- **Database Engine:** MySQL 8.x Community Server (Production/Dev)
- **Database Migrations:** Flyway (Version-controlled schema)
- **Test Database:** H2 (In-memory MySQL compatibility mode for unit/integration tests)
- **Testing:** JUnit 5, Mockito, MockMvc
- **Frontend:** HTML5, CSS3, JavaScript (ES6+), Bootstrap 5.3.3

---

## 4. Local Windows 10/11 Setup Guide

### 4.1 Prerequisites
Verify in PowerShell that Java 21 and Maven 3.9+ are installed:
```powershell
java -version
mvn -version
```

### 4.2 Database Initialization
Open MySQL Command Line Client or MySQL Workbench and run:
```sql
CREATE DATABASE IF NOT EXISTS college_resource_finder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```
Or execute the provided SQL script:
```powershell
mysql -u root -p < scripts\setup-database.sql
```

### 4.3 Environment Variables
Copy `.env.example` to `.env` or set environment variables in your Windows session:
```powershell
# Set your MySQL root password for the session
$env:DB_PASSWORD = "your_mysql_password"

# Optional overrides (defaults shown)
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "college_resource_finder"
$env:DB_USERNAME = "root"
$env:SERVER_PORT = "8080"
$env:STORAGE_DIR = "../storage"
```

### 4.4 Build Verification
Run the automated test suite from the `backend/` directory:
```powershell
cd backend
mvn clean test
```

### 4.5 Running the Application
Using the provided PowerShell script:
```powershell
.\scripts\start-backend.ps1
```
Or via Maven directly:
```powershell
cd backend
mvn spring-boot:run
```

Once started:
- **Web Application:** [http://localhost:8080](http://localhost:8080)
- **Sign In / Register:** [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Academic Master Data:** [http://localhost:8080/master-data.html](http://localhost:8080/master-data.html)
- **Academic Resources:** [http://localhost:8080/resources.html](http://localhost:8080/resources.html)
- **Search & History:** [http://localhost:8080/search.html](http://localhost:8080/search.html)
- **Health API:** [http://localhost:8080/api/v1/health](http://localhost:8080/api/v1/health)
- **Spring Boot Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 5. Authentication, Roles & Security (Module 02)

### 5.1 Roles and Boundaries
| Role | Authority | Access Boundaries |
| :--- | :--- | :--- |
| **Student** | `ROLE_STUDENT` | Search courseware, download files, view personal attendance, manage personal profile. Read-only master data. |
| **Faculty / Staff** | `ROLE_FACULTY` | All student capabilities + upload resources, mark daily class attendance, controlled file-saving. Read-only master data. |
| **Administrator** | `ROLE_ADMIN` | Full institutional authority: manage users, assign roles, deactivate accounts, manage academic master data, inspect audit logs. |

### 5.2 Initial Administrator Provisioning
The application seeds an initial administrator account automatically via Flyway migration `V2__seed_master_data.sql`:
- **Default Email:** `admin@college.edu`
- **Default Password:** `Admin@123` *(Hashed with BCrypt work factor 12)*

> **Security Note:** Rotate this password upon first login either through `PUT /api/v1/auth/me` or by updating the database record. The primary administrator account is safeguarded against accidental self-deactivation.

### 5.3 Public Registration Policy
Public registration (`POST /api/v1/auth/register`) strictly forces `role = STUDENT`. Role assignment for `FACULTY` or `ADMIN` is strictly reserved for authenticated administrators via `PATCH /api/v1/admin/users/{id}/status`.

### 5.4 Authentication API Endpoints
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Public | Authenticates credentials and issues `JSESSIONID` cookie |
| `POST` | `/api/v1/auth/register` | Public | Self-registration (strictly `STUDENT` role) |
| `POST` | `/api/v1/auth/logout` | Authenticated | Invalidates session and clears cookies |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieves current user profile |
| `PUT` | `/api/v1/auth/me` | Authenticated | Updates display name or rotates password |
| `GET` | `/api/v1/auth/csrf` | Public | Retrieves active CSRF token details |
| `GET` | `/api/v1/admin/users` | `ROLE_ADMIN` | Lists users with pagination and role filters |
| `PATCH`| `/api/v1/admin/users/{id}/status`| `ROLE_ADMIN` | Activates/deactivates accounts or assigns roles |

---

## 6. Academic Master Data Management (Module 03)

Module 03 establishes the relational academic taxonomy governing all downstream learning resources and attendance records.

### 6.1 Entities Managed
1. **Departments:** Academic branches (e.g., MCA, CS, IT, Commerce, Mathematics) with unique department codes.
2. **Semesters:** Curricular semesters 1 through 8 with `ODD` or `EVEN` term designations.
3. **Subjects:** Subject titles and unique codes tied to a parent Department and Semester number (1 to 8).
4. **Units:** Course syllabus units (1 to 10) mapped uniquely to each subject (`UNIQUE(subject_id, unit_number)`).
5. **Resource Categories:** Classification tags (Lecture Notes, Question Banks, Assignments, E-Books, Lab Manuals, Syllabus, Research Papers).

### 6.2 Data Integrity & Security Rules
- **Non-Destructive Deactivations:** Soft deactivation (`is_active = FALSE`) preserves historical resource relationships when programs or subjects are retired.
- **Role-Based Access Control:** Students and faculty have read-only access (`GET`); write, update, and deactivation operations (`POST`, `PUT`, `PATCH`) are strictly restricted to `ROLE_ADMIN`.
- **Validation & Duplicate Prevention:** Validated via Bean Validation and JPA unique constraints preventing duplicate codes, names, or overlapping unit numbers.

### 6.3 Master Data API Endpoints (`/api/v1/master`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/master/departments` | Authenticated | Paginated & searchable department list |
| `GET` | `/api/v1/master/departments/active` | Authenticated | Active departments lookup list |
| `GET` | `/api/v1/master/departments/{id}` | Authenticated | Single department details |
| `POST` | `/api/v1/master/departments` | `ROLE_ADMIN` | Create new department |
| `PUT` | `/api/v1/master/departments/{id}` | `ROLE_ADMIN` | Update department details |
| `PATCH`| `/api/v1/master/departments/{id}/status` | `ROLE_ADMIN` | Activate or deactivate department |
| `GET` | `/api/v1/master/semesters` | Authenticated | All semesters (1-8) |
| `POST` | `/api/v1/master/semesters` | `ROLE_ADMIN` | Create semester record |
| `PATCH`| `/api/v1/master/semesters/{id}/status` | `ROLE_ADMIN` | Toggle semester active status |
| `GET` | `/api/v1/master/subjects` | Authenticated | Paginated subjects (filter by department, semester, keyword) |
| `GET` | `/api/v1/master/subjects/{id}` | Authenticated | Subject details (optional `includeUnits=true`) |
| `POST` | `/api/v1/master/subjects` | `ROLE_ADMIN` | Create new subject |
| `PUT` | `/api/v1/master/subjects/{id}` | `ROLE_ADMIN` | Update subject |
| `PATCH`| `/api/v1/master/subjects/{id}/status` | `ROLE_ADMIN` | Toggle subject active status |
| `GET` | `/api/v1/master/subjects/{id}/units`| Authenticated | Ordered units (1..10) for a subject |
| `POST` | `/api/v1/master/subjects/{id}/units`| `ROLE_ADMIN` | Add syllabus unit to a subject |
| `PUT` | `/api/v1/master/units/{unitId}` | `ROLE_ADMIN` | Update unit details |
| `PATCH`| `/api/v1/master/units/{unitId}/status` | `ROLE_ADMIN` | Toggle unit active status |
| `GET` | `/api/v1/master/categories` | Authenticated | Paginated & searchable categories |
| `GET` | `/api/v1/master/categories/active` | Authenticated | Active categories lookup list |
| `POST` | `/api/v1/master/categories` | `ROLE_ADMIN` | Create new resource category |
| `PUT` | `/api/v1/master/categories/{id}` | `ROLE_ADMIN` | Update category details |
| `PATCH`| `/api/v1/master/categories/{id}/status`| `ROLE_ADMIN` | Toggle category active status |

---

## 7. Academic Resource Repository (Module 04)

Module 04 implements the central academic content repository, coupling relational metadata in MySQL with controlled, path-traversal-guarded local disk storage.

### 7.1 Resource Classification & Types
- **Lecture notes:** Classroom presentations, course slides, and curated instructor notes.
- **Study materials:** Revision handbooks, chapter digests, and core curriculum summaries.
- **Assignments:** Laboratory exercise briefs, homework problem sets, and practical assignments.
- **Previous examination papers:** Mid-semester, end-semester, and model question papers with answer schemes.
- **E-books & reference documents:** Prescribed textbook references, university regulations, and academic papers.
- **Other approved academic resources:** Supplementary laboratory codes, datasets, and guides.

### 7.2 Stored Metadata Model
- **Core Info:** Title, detailed description, resource classification type, summary, and AI-origin flags.
- **Academic Taxonomy:** Foreign keys to Department, Subject, Curricular Semester, and optional Unit (1..10).
- **Physical File & Storage:** Original filename, server-generated unique storage filename (`res_<uuid>.<ext>`), partitioned relative path (`{DEPT}/SEM{sem}/{SUBJ}/`), file extension, byte size, and formatted size.
- **Ownership & Provenance:** Foreign key to uploader account (`users`), uploader name, role, upload date, and update date.
- **Lifecycle & Security:** Status (`ACTIVE`, `ARCHIVED`, `DELETED`), `is_active` soft-deletion flag, and full audit logging.

### 7.3 Security & Role-Based Ownership Matrix
- **Browse & View:** `ROLE_STUDENT`, `ROLE_FACULTY`, and `ROLE_ADMIN` can list resources with multi-faceted filtering, view details, preview documents, and download files.
- **Upload:** Restricted to `ROLE_FACULTY` and `ROLE_ADMIN`.
- **Edit & Soft-Delete:** Restricted to the original uploader (`ROLE_FACULTY`) or system administrators (`ROLE_ADMIN`). Non-owner faculty and students are rejected with HTTP 403 Forbidden.
- **Storage Protection:** Users cannot supply arbitrary file paths. All file access is resolved strictly by numeric database ID inside the configured storage root, preventing path traversal attacks.

### 7.4 Resource Repository API Endpoints (`/api/v1/resources`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/resources` | Authenticated | Multi-faceted filter & paginated listing (dept, sem, subj, unit, cat, type, search) |
| `GET` | `/api/v1/resources/{id}` | Authenticated | Retrieve complete resource metadata details |
| `GET` | `/api/v1/resources/{id}/recommendations` | Authenticated | Retrieve deterministic rule-based related resource recommendations |
| `POST` | `/api/v1/resources` | `ROLE_FACULTY`, `ROLE_ADMIN` | Upload resource file and persist academic taxonomy metadata |
| `PUT` | `/api/v1/resources/{id}` | Owner or `ROLE_ADMIN` | Update resource title, description, subject, unit, category |
| `DELETE`| `/api/v1/resources/{id}` | Owner or `ROLE_ADMIN` | Soft-deactivate resource (`status = DELETED`, `is_active = FALSE`) |
| `PATCH`| `/api/v1/resources/{id}/status`| Owner or `ROLE_ADMIN` | Update status (`ACTIVE`, `ARCHIVED`, `DELETED`) |
| `GET` | `/api/v1/resources/{id}/download`| Authenticated | Securely stream file as attachment |
| `GET` | `/api/v1/resources/{id}/preview` | Authenticated | Securely stream file inline for browser preview |
| `DELETE`| `/api/v1/resources/{id}/purge` | `ROLE_ADMIN` | Permanently purge database record and physical disk file |

---

## 8. Secure Resource File Handling & Storage Engine (Module 05)

Module 05 implements the defense-in-depth secure file handling subsystem, ensuring files are validated, isolated, and streamed without exposing internal server paths or risking arbitrary filesystem manipulation.

### 8.1 Multi-Tier File Validation Pipeline
- **Binary Signature (Magic Bytes):** Rather than trusting client-supplied extensions or Content-Type headers, the engine inspects binary headers directly:
  - **PDF:** Header must start with `%PDF` (`0x25, 0x50, 0x44, 0x46`).
  - **DOCX / PPTX / XLSX:** Header must match the PK ZIP container (`0x50, 0x4B, 0x03, 0x04`).
  - **TXT / CSV:** Checked for absence of binary NUL characters (`0x00`).
- **MIME Type Consistency:** Verifies that declared browser MIME headers are compatible with the physical file signature.
- **Configurable Size Limits:** Enforces configurable boundaries via `app.storage.max-file-size-mb=50` and `spring.servlet.multipart.max-file-size=50MB`.
- **Path Sanitization:** Filenames containing path traversal sequences (`..`, `/`, `\`) are rejected immediately with clear error responses.

### 8.2 Compensating Transaction Cleanup
- When a new resource is uploaded, physical file write and database persistence occur within an orchestrated transactional workflow.
- If a database failure or constraint violation occurs after the physical file has been saved to disk, an automated **compensating cleanup** hook purges the newly written file from disk, guaranteeing zero uncontrolled orphan files.

### 8.3 Download & Preview Defensive Controls
- **Authorization Enforcement:** Only authenticated sessions can request downloads or previews. Deactivated/deleted files are invisible to non-owners and students, returning generic 404 responses without leaking existence or metadata.
- **Defensive Headers:** All download responses inject `X-Content-Type-Options: nosniff`, `Cache-Control: private, no-transform`, and strict attachment content dispositions.
- **Safe Missing File Handling:** If a physical file is missing from disk, the server returns HTTP 404 with a safe generic error (`The requested physical file is not available on the server`), completely masking absolute Windows drive paths.

---

## 9. Database-Backed Resource Search & History (Module 06)

Module 06 implements deterministic, AI-free resource discovery combining keyword normalization, taxonomy facet extraction, relevance ranking, and private per-user search history.

### 9.1 Deterministic Natural Query Parsing
The search engine parses natural academic queries without requiring external NLP models:
- **Semester Extraction:** Resolves patterns like `Semester 2`, `Sem 2`, or `S2` to target semester `2`.
- **Unit Extraction:** Resolves patterns like `Unit 3` or `U3` to syllabus unit number `3`.
- **Category & Type Resolution:** Resolves keywords like `notes`/`slides` to *Lecture notes*, `question papers`/`pyq` to *Previous examination papers*, `assignments`/`lab` to *Assignments*, and `books`/`e-books` to *E-books & reference documents*.
- **Departmental Scoping:** Resolves queries mentioning `my department` to the authenticated user's registered department ID.
- **Acronym & Keyword Matching:** Matches `DBMS`, `Java`, `OS`, `DSA`, etc., to subject codes and titles while filtering common English stopwords.

### 9.2 Search History Subsystem
- **Private Query Logging:** Each authenticated search is recorded with timestamp and result count.
- **Ownership & Privacy:** Users can only view, re-run, or delete their own search queries (`findByUser_UserId`).
- **History Management:** Provides endpoints to retrieve recent queries (`GET /api/v1/search/history`), delete individual entries (`DELETE /api/v1/search/history/{id}`), or clear history completely (`DELETE /api/v1/search/history`).

### 9.3 Search & Discovery API Endpoints (`/api/v1/search`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/search` | Authenticated | Multi-faceted search with deterministic natural query parsing |
| `GET` | `/api/v1/search/history` | Authenticated | Retrieve authenticated user's private search history |
| `DELETE`| `/api/v1/search/history/{id}` | Authenticated | Delete a specific search history entry owned by current user |
| `DELETE`| `/api/v1/search/history` | Authenticated | Clear all search history for current user |

---

## 10. Rule-Based Resource Recommendations (Module 07)

Module 07 implements a deterministic, multi-criteria recommendation engine without external AI services or embeddings. It generates repeatable recommendations using MySQL metadata similarity and taxonomy relationships.

### 10.1 Deterministic Relevance Scoring Model
Each candidate resource is evaluated against the source resource across 7 dimensions:

| Criterion | Weight | Rationale |
| :--- | :---: | :--- |
| **Same Subject** | `+40` | Strongest academic relevance indicator |
| **Same Syllabus Unit** | `+20` | High contextual topical relevance within the same subject |
| **Same Department** | `+15` | Departmental alignment across adjacent subjects |
| **Same Category** | `+10` | Aligned resource format (e.g., Question Paper to Question Paper) |
| **Same Semester** | `+10` | Academic stage parity (e.g., Semester 2 to Semester 2) |
| **Same Resource Type** | `+5` | Structural resource similarity |
| **Shared Title Keywords** | `+5 each (max +25)` | Common non-stopword terminology |

### 10.2 Candidate Pre-Filtering & Safety Guarantees
- **Database-Level Candidate Filtering:** Queries active resources sharing at least one dimension (`subject`, `department`, `category`, `semester`) with a bounded limit (`MAX_CANDIDATES = 50`) before in-memory scoring.
- **Strict Self-Exclusion:** The source resource is guaranteed never to appear in its own recommendations.
- **Deactivated Resource Exclusion:** Inactive (`is_active = FALSE`) and deleted (`status = 'DELETED'`) resources are strictly omitted.
- **Access Control:** Enforces authenticated session boundaries under `/api/v1/resources/{id}/recommendations`.
- **Bounded Output:** Default 10 recommendations, clamped to a maximum ceiling of 20.
- **Empty States:** Gracefully returns empty list with HTTP 200 for isolated resources with zero peer matches.
- **AI-Extensible Architecture:** Encapsulated in `RecommendationService` with `RecommendationResponse`, ready for future vector or hybrid reranking in Module 12 without changing REST contracts.

---

## 11. Staff Attendance Search & Calculations (Module 08)

Module 08 implements the staff attendance subsystem, providing authorized data entry, batch imports with origin tracking, multi-criteria roster searching, and deterministic attendance percentage metrics.

### 11.1 Attendance Calculation Formula & Policy
Attendance metrics are derived strictly from database records without estimation:

$$\text{Attendance Percentage} = \left(\frac{\text{Sessions Attended}}{\text{Total Applicable Sessions}}\right) \times 100$$

- **Total Applicable Sessions:** Total recorded duty sessions scheduled for the staff member in the evaluated date range (`PRESENT + ON_DUTY + ABSENT + LEAVE`).
- **Sessions Attended:** Sessions where staff fulfilled duty (`PRESENT + ON_DUTY`).
- **Zero-Session Handling:** When `Total Applicable Sessions = 0` (e.g. holiday window or unrecorded period), percentage safely returns `0.00%` (preventing division-by-zero errors).

### 11.2 Confidentiality & Multi-Role Access Control
- **Student Privacy Barrier:** Staff attendance records and roster statistics are strictly confidential. Ordinary students (`ROLE_STUDENT`) are blocked with HTTP 403 Forbidden.
- **Faculty Access:** `ROLE_FACULTY` members can search records, view department summaries, and submit class/session attendance logs.
- **Administrator Authority:** `ROLE_ADMIN` holds institutional governance: batch CSV imports, single entries, record updates, and deletions.
- **Audit Logging:** Every sensitive operation (single record creation, batch imports, record modifications, and deletions) writes a persistent entry into `audit_logs`.

### 11.3 Attendance Subsystem API Endpoints (`/api/v1/attendance`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/attendance/staff` | `ROLE_FACULTY`, `ROLE_ADMIN` | Paginated search (staffId, departmentId, date range, status) |
| `GET` | `/api/v1/attendance/staff/{staffId}/summary` | `ROLE_FACULTY`, `ROLE_ADMIN` | Calculate attendance metrics, percentage, and session breakdown |
| `POST` | `/api/v1/attendance/staff` | `ROLE_FACULTY`, `ROLE_ADMIN` | Record single staff attendance entry with duplicate protection |
| `POST` | `/api/v1/attendance/staff/import` | `ROLE_FACULTY`, `ROLE_ADMIN` | Batch import records with data origin tracking (`CSV_IMPORT`, etc.) |
| `PUT` | `/api/v1/attendance/staff/{id}` | `ROLE_FACULTY` (recorder), `ROLE_ADMIN` | Update attendance status or remarks |
| `DELETE`| `/api/v1/attendance/staff/{id}` | `ROLE_ADMIN` | Delete attendance record (audited) |

---

## 12. Controlled File-Saving Workflows (Module 09)

Module 09 implements deterministic file organization and saving from existing authorized resources into pre-approved, allowlisted institution storage categories without AI.

### 12.1 Security & Allowlisted Destination Categories
Files can only be organized into predefined, approved categories within the fixed application storage root (`app.storage.upload-dir`):

| Approved Category | Target Storage Directory | Description |
| :--- | :--- | :--- |
| `LECTURE_NOTES` | `organized/lecture_notes/` | Classroom lecture slides and unit-wise course notes |
| `QUESTION_PAPERS` | `organized/question_papers/` | University and internal test question papers |
| `ASSIGNMENTS` | `organized/assignments/` | Student homework, lab assignments, and project prompts |
| `LAB_MANUALS` | `organized/lab_manuals/` | Laboratory exercise guides and software installation docs |
| `STUDY_MATERIALS` | `organized/study_materials/` | Reference textbooks, e-books, and research publications |
| `SYLLABUS` | `organized/syllabus/` | Curriculum structure, semester-wise syllabus, and course outcomes |
| `ARCHIVES` | `organized/archives/` | Archived historical materials and previous curriculum iterations |

### 12.2 Path Traversal & Collision Defenses
- **Fixed Storage Root:** All destination paths are verified to reside under `rootLocation.toAbsolutePath()`.
- **Strict Path Sanitation:** Clients never provide raw filesystem paths. Optional subfolder tags are validated against regex `^[a-zA-Z0-9_-]{1,50}$`. Any traversal sequences (`..`, `/`, `\`) are rejected immediately.
- **Collision-Free Server Naming:** Generates safe server-side filenames (`saved_{uuid}_{sanitizedName}`).
- **Overwrite Authorization:** Overwriting existing destination files is blocked unless explicitly authorized (`overwrite = true`) by an administrator or resource owner.
- **Compensating Cleanup:** If MySQL metadata persistence fails after physical file write, the newly written file is immediately deleted, preventing orphan files.

### 12.3 Controlled File-Saving API Endpoints (`/api/v1/files/controlled-save`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/files/controlled-save/categories` | `ROLE_FACULTY`, `ROLE_ADMIN` | List approved allowlisted destination categories |
| `POST` | `/api/v1/files/controlled-save` | `ROLE_FACULTY`, `ROLE_ADMIN` | Execute controlled file copy and record metadata |
| `GET` | `/api/v1/files/controlled-save/history` | `ROLE_FACULTY`, `ROLE_ADMIN` | Paginated history of file saving operations |
| `GET` | `/api/v1/files/controlled-save/{operationId}` | `ROLE_FACULTY`, `ROLE_ADMIN` | Retrieve single operation details |

---

## 13. Administration, Reporting & Audit Logging (Module 10)

Module 10 implements centralized administrative oversight, governance, real-time database-driven reporting, and security audit log inspection without fabricated statistics.

### 13.1 Governance & Reporting Architecture
- **Zero Fabricated Statistics:** All system metrics (users, active resources, storage consumption, attendance records, search counts, file operations) are computed in real time from SQL aggregate queries.
- **Strict Server-Side RBAC:** All endpoints under `/api/v1/admin/**` require `ROLE_ADMIN`. Students and faculty are strictly rejected with HTTP 403 Forbidden.
- **Account Governance:** Safe role modification (`STUDENT`, `FACULTY`, `ADMIN`) and active/deactivated toggling with protection against self-deactivation and default administrator protection.
- **Immutable Security Audit Log:** Complete event logging for sensitive operations (user account changes, master data mutations, resource deletions) recording actor, action, target entity, client IP, status, and details (without logging secrets, tokens, or file contents).

### 13.2 Administrative API Endpoints (`/api/v1/admin`)
| Method | Endpoint | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/reports/dashboard` | `ROLE_ADMIN` | Aggregated system KPIs (users, resources, storage, searches) |
| `GET` | `/api/v1/admin/reports/departments` | `ROLE_ADMIN` | Resource & storage distribution by academic department |
| `GET` | `/api/v1/admin/reports/semesters` | `ROLE_ADMIN` | Resource distribution by semester level |
| `GET` | `/api/v1/admin/reports/categories` | `ROLE_ADMIN` | Resource distribution by academic category |
| `GET` | `/api/v1/admin/reports/search-activity` | `ROLE_ADMIN` | Top search queries from actual history |
| `GET` | `/api/v1/admin/audit-logs` | `ROLE_ADMIN` | Filterable, paginated audit records (action, status, date) |
| `GET` | `/api/v1/admin/users` | `ROLE_ADMIN` | Paginated user accounts list with status and role |
| `GET` | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` | Detailed user account profile |
| `PATCH`| `/api/v1/admin/users/{id}/status` | `ROLE_ADMIN` | Update account active flag or reassign user role |

---

## 14. Non-AI Integration & Quality Assurance (Module 11)

Module 11 executes rigorous end-to-end integration and security verification of all 12 non-AI functional workflows before introducing AI/NLP capabilities.

- **Automated Test Suite:** 119 unit and integration tests passing (`BUILD SUCCESS`) with 0 failures, 0 errors, and 0 skipped tests.
- **Security Assessment:** Enforced defenses across authentication, session fixation, CSRF tokens (`X-XSRF-TOKEN`), path traversal prevention, magic bytes binary signature validation, parameterized SQL injection immunity, and server-side query bounding.
- **Artifact Packaging:** Application successfully repackages into an executable standalone fat JAR (`resource-finder-1.0.0-SNAPSHOT.jar`).

---

## 15. AI/NLP Integration Architecture (Module 12 — Final Module)

Module 12 introduces intelligent conversational discovery, document summarization, hybrid natural-language query processing, and semantic vector ranking optimized specifically for **Qwen3-0.6B** running on resource-constrained local hardware (e.g. 8GB RAM Windows laptops).

### 15.1 Architecture & Hardware Isolation
```
┌────────────────────────────────────────────────────────┐
│                   Client Browser                       │
│    (HTML5 / Bootstrap 5 / Vanilla ES6 JavaScript)      │
└───────────────────────────┬────────────────────────────┘
                            │ REST / JSON (Session + CSRF)
                            ▼
┌────────────────────────────────────────────────────────┐
│            Spring Boot 3.4.1 Application               │
│   (Single Source of Truth: MySQL 8.x + Storage Root)   │
│   - Enforces RBAC & Multi-Role Permissions             │
│   - Rate Limiter (30 req/min) & Input Size Bounding    │
│   - Fast Failover to In-JVM Fallback Engine            │
└───────────────────────────┬────────────────────────────┘
                            │ REST (FastAPI / 127.0.0.1:8000)
                            ▼
┌────────────────────────────────────────────────────────┐
│             Python FastAPI AI Microservice             │
│   - Hybrid Query Parser (Regex + LLM Few-Shot)         │
│   - Schema Validation Layer (Semesters 1-8, Units 1-10)│
│   - Text Chunking & Extractive Pre-filtering           │
│   - Vector Cosine Similarity Engine                    │
│   - Prompt Injection Guardrails                        │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP POST (keep_alive: "2m")
                            ▼
┌────────────────────────────────────────────────────────┐
│              Local Ollama Daemon (11434)               │
│                  Model: qwen3:0.6b                     │
│       (Ultra-compact ~400MB memory footprint)          │
└────────────────────────────────────────────────────────┘
```

### 15.2 Key Capabilities & Defensive Safeguards
1. **Hybrid Query Parsing:** Combines zero-latency regex extraction with few-shot Qwen3-0.6B semantic refinement. All outputs are strictly validated against database schemas (valid semesters 1–8, valid units 1–10, and allowlisted categories).
2. **Context Window Chunking:** Large multi-page documents (>550 words) are condensed via extractive pre-filtering before feeding into Qwen3-0.6B's 2048-token context, eliminating context truncation and hallucination.
3. **Architectural Separation of Vector Search:** Generative 0.6B models are never used to compute vector embeddings. Dedicated embedding support (SentenceTransformers `all-MiniLM-L6-v2`, Ollama embeddings, or lightweight subword TF-IDF vector cosine engine) is used.
4. **Deterministic Fallbacks:** If Ollama or the Python service is offline, unresponsive, or times out (>5-8s), the platform falls back instantaneously to deterministic internal engines with zero downtime and zero impact on primary resource management.
5. **Prompt Injection Immunity:** User input is treated as untrusted and screened against jailbreaks, instruction overrides, credential leaks, and shell/SQL injection attempts.

### 15.3 AI Subsystem API Endpoints
| Service | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **FastAPI** | `GET` | `/health` | Diagnostic status (Ollama connection, model availability, active embedding engine) |
| **FastAPI** | `POST` | `/api/v1/ai/parse-query` | Hybrid entity extraction (subject, semester, unit, category, department) |
| **FastAPI** | `POST` | `/api/v1/ai/summarize` | Document chunking and Qwen3-0.6B generative summarization |
| **FastAPI** | `POST` | `/api/v1/ai/semantic-search` | Vector cosine similarity scoring across authorized candidates |
| **FastAPI** | `POST` | `/api/v1/ai/chat` | Controlled conversational assistant dispatching tools |
| **FastAPI** | `POST` | `/api/v1/ai/parse-file-save` | Allowlisted parameters for controlled file saving |
| **Spring Boot** | `GET` | `/api/v1/ai/health` | Overall AI gateway status and provider mode (`FASTAPI` vs `FALLBACK`) |
| **Spring Boot** | `POST` | `/api/v1/ai/parse-query` | Authenticated natural-language query parser |
| **Spring Boot** | `POST` | `/api/v1/ai/search` | AI-augmented search merging NLP criteria with JPA specifications |
| **Spring Boot** | `POST` | `/api/v1/ai/semantic-search` | Authorized semantic search with SQL fallback |
| **Spring Boot** | `POST` | `/api/v1/ai/resources/{id}/summarize` | Role-verified document summarization with MySQL SHA-256 caching |
| **Spring Boot** | `POST` | `/api/v1/ai/chat` | Conversational assistant with database audit logging |
| **Spring Boot** | `POST` | `/api/v1/ai/parse-file-save` | Faculty/Admin natural-language file organization translator |

---

## 16. Local Windows Setup Guide (Compatible with 8GB RAM Laptops)

### 16.1 Prerequisites
- **OS:** Windows 10 / 11 (64-bit)
- **RAM:** Minimum 8GB system RAM
- **Java:** Java 21 LTS (`java -version`)
- **Maven:** Apache Maven 3.9+ (`mvn -version`)
- **Python:** Python 3.10 to 3.12 (`python --version`)
- **Ollama:** Ollama for Windows (download from https://ollama.com)

### 16.2 Python AI Microservice Setup
Open PowerShell in the project root:
```powershell
cd ai_service

# 1. Create and activate virtual environment
python -m venv venv
.\venv\Scripts\Activate.ps1

# 2. Install dependencies
pip install -r requirements.txt
```

### 16.3 Running Ollama with Qwen3-0.6B
In a separate terminal:
```powershell
# Pull and test run the compact 0.6B model (~400MB)
ollama run qwen3:0.6b

# Or keep Ollama serving in the background:
ollama serve
```

### 16.4 Environment Configuration
Set environment variables (optional; defaults are preconfigured for 8GB RAM laptops):
```powershell
$env:OLLAMA_HOST = "http://127.0.0.1:11434"
$env:OLLAMA_MODEL = "qwen3:0.6b"
$env:OLLAMA_KEEP_ALIVE = "2m"          # Unload model after 2 minutes idle
$env:OLLAMA_TIMEOUT = "8.0"           # Strict HTTP timeout in seconds
$env:AI_SERVICE_PORT = "8000"
$env:AI_DISABLE_LLM = "false"         # Set 'true' to force 100% deterministic fallback
```

### 16.5 Starting the AI Microservice
```powershell
cd ai_service
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```
Verify health check in your browser or PowerShell:
```powershell
curl http://localhost:8000/health
```
Expected output:
```json
{
  "status": "UP",
  "service": "ai-college-resource-finder",
  "version": "1.0.0",
  "target_model": "qwen3:0.6b",
  "ollama_host": "http://127.0.0.1:11434",
  "ollama_connected": true,
  "model_available": true,
  "embedding_provider": "subword_tfidf_cosine_engine",
  "fallback_ready": true,
  "hardware_profile": "8GB_RAM_OPTIMIZED"
}
```

### 16.6 Running the Test Suites
Run the Python test suite:
```powershell
cd ai_service
python test_ai_service.py
```
Run the Spring Boot test suite:
```powershell
cd backend
mvn test
```

### 16.7 Troubleshooting & Memory Management for 8GB RAM
- **Ollama High Memory / Lag:** Ensure `OLLAMA_KEEP_ALIVE` is set to `"2m"`. This instructs Ollama to immediately release VRAM/RAM after processing queries instead of holding the model continuously in memory.
- **Ollama Service Unreachable:** If Ollama is not running, the application will not fail or crash. Both the Python FastAPI service and Spring Boot backend automatically switch to deterministic in-JVM / rule-based fallbacks with zero latency.
- **Model Load Delay on First Query:** On older laptops or mechanical hard drives, the very first query to Ollama may take 3-5 seconds while reading model weights. Subsequent queries complete in ~200-500ms.

---

## 17. Module Implementation Roadmap

| Module | Name | Status |
| :---: | :--- | :---: |
| **00** | System Requirements & Architecture Specification | ✅ Complete |
| **01** | Project Foundation & Build System | ✅ Complete |
| **02** | Authentication, Authorization & User Management (RBAC) | ✅ Complete |
| **03** | Academic Master Data Management (Departments, Subjects, Categories) | ✅ Complete |
| **04** | Resource Repository & Metadata Management | ✅ Complete |
| **05** | Secure Resource File Handling & Storage Engine | ✅ Complete |
| **06** | Database-Backed Search & Search History Tracking | ✅ Complete |
| **07** | Related-Resource Recommendations | ✅ Complete |
| **08** | Staff Attendance Search & Calculations | ✅ Complete |
| **09** | Controlled File-Saving Workflows | ✅ Complete |
| **10** | Administration, Reports & Audit Logging | ✅ Complete |
| **11** | Non-AI Integration & Quality Assurance Review | ✅ Complete |
| **12** | AI/NLP Integration (Optimized for Qwen3-0.6B Local LLM) | ✅ Complete (Final Module) |
