-- ==============================================================================
-- Database Setup Script: AI-Based College Resource Finder
-- Target: MySQL 8.x Community Server (Local Windows)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS college_resource_finder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE college_resource_finder;

SELECT 'Database college_resource_finder verified and ready for Flyway migrations.' AS Status;
