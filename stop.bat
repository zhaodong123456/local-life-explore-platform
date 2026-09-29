@echo off

echo.
echo ========================================
echo   Explore Platform - Stopping
echo ========================================
echo.

netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if %errorlevel% neq 0 goto skip_backend
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>&1
    echo [Backend] closed PID %%a port 8080
)
goto backend_done
:skip_backend
echo [Backend] port 8080 not listening, skip
:backend_done

netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if %errorlevel% neq 0 goto skip_frontend
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /PID %%a /F >nul 2>&1
    echo [Frontend] closed PID %%a port 5173
)
goto frontend_done
:skip_frontend
echo [Frontend] port 5173 not listening, skip
:frontend_done

echo.
echo ========================================
echo   Done. MySQL/Redis/RabbitMQ untouched
echo ========================================
echo.
ping -n 4 127.0.0.1 >nul