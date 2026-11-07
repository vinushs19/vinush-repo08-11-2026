@echo off
setlocal enabledelayedexpansion

echo ==============================================================================
echo  AI-Based College Resource Finder - Windows Backend Startup
echo ==============================================================================

:: Locate project directories
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "BACKEND_DIR=%PROJECT_ROOT%\backend"

:: Locate Maven
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin;%PATH%"
    ) else (
        echo [ERROR] Maven not found in PATH or at %USERPROFILE%\tools\maven\apache-maven-3.9.9\bin
        echo Please ensure Apache Maven 3.9+ is installed.
        pause
        exit /b 1
    )
)

:: Verify Java
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found. Please install JDK 21 and configure JAVA_HOME.
    pause
    exit /b 1
)

:: Prompt for DB_PASSWORD if not already set
if "%DB_PASSWORD%"=="" (
    echo [NOTE] DB_PASSWORD is not set in environment.
    set /p DB_PASSWORD="Enter MySQL root password (or press Enter if blank): "
)

cd /d "%BACKEND_DIR%"
echo [INFO] Starting Spring Boot application from %BACKEND_DIR%...
call mvn spring-boot:run
pause
