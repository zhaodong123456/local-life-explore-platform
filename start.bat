@echo off

set "JAVA_HOME=D:\Microservice\jdk\jdk17"
set "NODE_HOME=D:\Microservice\node.js\node.js-22.22.3"
set "ERLANG_HOME=D:\Microservice\Erlang OTP 28"
set "RABBITMQ_HOME=D:\Microservice\RabbitMQ Server\rabbitmq_server-4.3.6"
set "MVN=D:\Microservice\maven\apache-maven-3.8.8\bin\mvn.cmd"
set "PROJECT=%~dp0"

echo.
echo ========================================
echo   Explore Platform - Starting
echo ========================================
echo.

rem ---- 0. RabbitMQ ----
netstat -ano | findstr ":5672" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto rabbitmq_ready
echo [RabbitMQ] starting with Erlang OTP 28 ...
set "PATH=%ERLANG_HOME%\bin;%RABBITMQ_HOME%\sbin;%PATH%"
start "rabbitmq" /min "%RABBITMQ_HOME%\sbin\rabbitmq-server.bat" -detached
set count=0
:wait_rabbitmq
ping -n 3 127.0.0.1 >nul
netstat -ano | findstr ":5672" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto rabbitmq_ready
set /a count=%count%+1
if %count% lss 15 goto wait_rabbitmq
echo [RabbitMQ] timeout, RabbitMQ may not be installed correctly
goto skip_rabbitmq
:rabbitmq_ready
echo [RabbitMQ] OK -^> port 5672
:skip_rabbitmq

rem ---- 1. Backend ----
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto backend_running
if exist "%PROJECT%backend\target\explore-platform-1.0.0.jar" goto start_backend
echo [Backend] jar not found, building...
call "%MVN%" -f "%PROJECT%backend\pom.xml" package -DskipTests -q
if not exist "%PROJECT%backend\target\explore-platform-1.0.0.jar" (
    echo [Backend] build failed, check Maven
    pause
    exit /b 1
)
echo [Backend] build done
:start_backend
echo [Backend] starting java -jar ...
pushd "%PROJECT%backend"
start "explore-backend" /min "%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -jar target\explore-platform-1.0.0.jar
popd
set count=0
:wait_backend
ping -n 3 127.0.0.1 >nul
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto backend_ready
set /a count=%count%+1
if %count% lss 20 goto wait_backend
echo [Backend] timeout, check MySQL/Redis/RabbitMQ
pause
exit /b 1
:backend_ready
echo [Backend] OK -^> http://localhost:8080
goto start_frontend
:backend_running
echo [Backend] port 8080 already listening, skip

:start_frontend
rem ---- 2. Frontend (direct node+vite) ----
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto frontend_running
echo [Frontend] starting vite ...
pushd "%PROJECT%frontend"
start "explore-frontend" /min "%NODE_HOME%\node.exe" "node_modules\vite\bin\vite.js"
popd
set count=0
:wait_frontend
ping -n 3 127.0.0.1 >nul
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 goto frontend_ready
set /a count=%count%+1
if %count% lss 15 goto wait_frontend
echo [Frontend] timeout, run npm install in frontend/
goto open_browser
:frontend_ready
echo [Frontend] OK -^> http://localhost:5173
goto open_browser
:frontend_running
echo [Frontend] port 5173 already listening, skip

:open_browser
rem ---- 3. Browser ----
echo.
echo Opening browser...
start http://localhost:5173
echo.
echo ========================================
echo   All ready!
echo   Frontend: http://localhost:5173
echo   API Doc:  http://localhost:8080/doc.html
echo   Stop:     double-click stop.bat
echo ========================================
echo.
echo Tip: stop.bat only closes Backend+Frontend.
echo RabbitMQ stays running for faster restart.
echo.
pause