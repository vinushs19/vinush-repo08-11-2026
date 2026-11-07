@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "BACKEND_DIR=%SCRIPT_DIR%..\backend"

where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin;%PATH%"
    )
)

cd /d "%BACKEND_DIR%"
echo [INFO] Compiling and running tests in %BACKEND_DIR%...
call mvn clean test
pause
