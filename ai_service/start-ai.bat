@echo off
setlocal enabledelayedexpansion
title College Resource Finder - AI Service (Port 8000)

echo ==============================================================================
echo       AI Microservice Launcher (FastAPI + Qwen3-0.6B Ollama)
echo ==============================================================================
echo.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

where python >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Python is not installed or not in your PATH.
    pause
    exit /b 1
)

:: Use virtual environment if present
if exist "venv\Scripts\python.exe" (
    echo [INFO] Activating virtual environment...
    call venv\Scripts\activate.bat
    set "PYTHON_CMD=python"
) else (
    set "PYTHON_CMD=python"
)

echo [INFO] Starting FastAPI service on http://localhost:8000 ...
%PYTHON_CMD% main.py

pause
