@echo off
setlocal enabledelayedexpansion
title AI-Based College Resource Finder - Launcher

echo ==============================================================================
echo       AI-Based College Resource Finder — Full Application Launcher
echo ==============================================================================
echo.

set "ROOT=%~dp0"
set "BACKEND_DIR=%ROOT%backend"
set "AI_DIR=%ROOT%ai_service"
set "JAR_FILE=%BACKEND_DIR%\target\resource-finder-1.0.0-SNAPSHOT.jar"

:: -----------------------------------------------------------------------------
:: 1. Check & Configure Java (JDK 21+)
:: -----------------------------------------------------------------------------
where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "C:\jdk-26.0.1\bin\java.exe" (
        set "PATH=C:\jdk-26.0.1\bin;%PATH%"
    ) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "PATH=C:\Program Files\Java\jdk-21\bin;%PATH%"
    ) else (
        echo [ERROR] Java not found. Please install JDK 21+ and add it to PATH.
        echo.
        pause
        exit /b 1
    )
)
echo [OK] Java detected.

:: -----------------------------------------------------------------------------
:: 2. Check & Configure Maven
:: -----------------------------------------------------------------------------
where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" (
        set "PATH=%USERPROFILE%\tools\maven\apache-maven-3.9.9\bin;%PATH%"
    )
)

:: -----------------------------------------------------------------------------
:: 3. Pre-flight Check: MySQL Server Status (Port 3306)
:: -----------------------------------------------------------------------------
echo [INFO] Checking MySQL service on localhost:3306...
powershell -NoProfile -Command "$t = New-Object System.Net.Sockets.TcpClient; try { $t.Connect('127.0.0.1', 3306); Write-Host 'OPEN'; $t.Close() } catch { Write-Host 'CLOSED' }" > "%TEMP%\mysql_port_check.txt" 2>nul
set /p MYSQL_PORT_STATUS=<"%TEMP%\mysql_port_check.txt"

if "%MYSQL_PORT_STATUS%"=="OPEN" goto :mysql_ok

echo [WARN] MySQL is not responding on port 3306.
echo [INFO] Attempting to start Windows MySQL service MySQL80...
net start MySQL80 >nul 2>nul

powershell -NoProfile -Command "$t = New-Object System.Net.Sockets.TcpClient; try { $t.Connect('127.0.0.1', 3306); Write-Host 'OPEN'; $t.Close() } catch { Write-Host 'CLOSED' }" > "%TEMP%\mysql_port_check.txt" 2>nul
set /p MYSQL_PORT_STATUS=<"%TEMP%\mysql_port_check.txt"

if "%MYSQL_PORT_STATUS%"=="OPEN" goto :mysql_ok
goto :mysql_error

:mysql_error
echo.
echo ==============================================================================
echo [ERROR] MySQL Service is STOPPED or unreachable on port 3306!
echo ==============================================================================
echo Spring Boot requires a running MySQL database server on port 3306.
echo.
echo To start MySQL, please do ONE of the following:
echo   1. Right-click 'run.bat' and choose 'Run as administrator'
echo   2. Open Services via Win+R, type 'services.msc', find 'MySQL80', and click 'Start'
echo   3. Open PowerShell as Administrator and run: Start-Service MySQL80
echo ==============================================================================
echo.
pause
exit /b 1

:mysql_ok
echo [OK] MySQL is active on port 3306.

:: -----------------------------------------------------------------------------
:: 4. Configure Database Credentials
:: -----------------------------------------------------------------------------
if "%DB_PASSWORD%"=="" (
    echo.
    set /p DB_PASSWORD="Enter MySQL root password (press Enter if blank/empty): "
)

:: -----------------------------------------------------------------------------
:: 5. Start Python AI Microservice (Port 8000)
:: -----------------------------------------------------------------------------
where python >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo.
    echo [INFO] Launching Python FastAPI AI Service in separate window...
    
    if exist "%AI_DIR%\venv\Scripts\python.exe" (
        set "PYTHON_EXE=%AI_DIR%\venv\Scripts\python.exe"
    ) else (
        set "PYTHON_EXE=python"
    )
    
    start "College Resource Finder - AI Service (Port 8000)" cmd /k "title AI Service (Port 8000) && cd /d "%AI_DIR%" && echo Starting AI Microservice on http://localhost:8000 ... && %PYTHON_EXE% main.py"
    
    :: Brief delay for AI microservice startup
    ping -n 3 127.0.0.1 >nul
) else (
    echo [WARN] Python not found on PATH. AI features will run via in-JVM fallback engine.
)

:: -----------------------------------------------------------------------------
:: 6. Launch Browser
:: -----------------------------------------------------------------------------
start "" "http://localhost:8080"

:: -----------------------------------------------------------------------------
:: 7. Start Spring Boot Application (Port 8080)
:: -----------------------------------------------------------------------------
echo.
echo ==============================================================================
echo [INFO] Starting Spring Boot Server on http://localhost:8080 ...
echo [INFO] Keep this window open. Press Ctrl+C to stop the application.
echo ==============================================================================
echo.

cd /d "%BACKEND_DIR%"

if exist "%JAR_FILE%" (
    echo [INFO] Running precompiled executable JAR...
    java -jar "%JAR_FILE%"
) else (
    echo [INFO] Compiling and running via Maven...
    call mvn spring-boot:run
)

echo.
echo [INFO] Application exited.
pause
