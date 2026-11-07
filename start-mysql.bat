@echo off
title Start MySQL Service
echo ==============================================================================
echo                      Starting MySQL 8.0 Service
echo ==============================================================================
echo.

:: Check for Administrator privileges
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [INFO] Requesting Administrator privileges to start MySQL...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

echo [INFO] Administrator rights confirmed.
echo [INFO] Starting MySQL80 service...
net start MySQL80
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] MySQL80 service is now RUNNING!
    echo [INFO] Setting MySQL80 to start automatically on Windows startup...
    sc config MySQL80 start= auto >nul 2>nul
) else (
    echo [WARN] Failed to start MySQL80 automatically.
    echo Please check Services (services.msc) or verify MySQL installation.
)

echo.
echo You can now run 'run.bat'.
echo.
pause
