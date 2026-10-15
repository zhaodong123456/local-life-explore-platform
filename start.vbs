' Explore Platform - Silent Launcher
' Double-click this file to start everything with NO windows

Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
dir = fso.GetParentFolderName(WScript.ScriptFullName)

' ---- 0. RabbitMQ ----
If Not CheckPort(5672) Then
    sh.Environment("Process").Item("ERLANG_HOME") = "D:\Microservice\Erlang OTP 28"
    sh.Environment("Process").Item("RABBITMQ_HOME") = "D:\Microservice\RabbitMQ Server\rabbitmq_server-4.3.6"
    oldPath = sh.Environment("Process").Item("PATH")
    sh.Environment("Process").Item("PATH") = "D:\Microservice\Erlang OTP 28\bin;D:\Microservice\RabbitMQ Server\rabbitmq_server-4.3.6\sbin;" & oldPath
    sh.Run """D:\Microservice\RabbitMQ Server\rabbitmq_server-4.3.6\sbin\rabbitmq-server.bat"" -detached", 0, False
    WaitPort 5672, 45
End If

' ---- 1. Backend ----
If Not CheckPort(8080) Then
    jarPath = dir & "\backend\target\explore-platform-1.0.0.jar"
    If Not fso.FileExists(jarPath) Then
        sh.Run "cmd /c ""D:\Microservice\maven\apache-maven-3.8.8\bin\mvn.cmd"" -f """ & dir & "\backend\pom.xml"" package -DskipTests -q", 0, True
    End If
    If fso.FileExists(jarPath) Then
        sh.CurrentDirectory = dir & "\backend"
        sh.Run """D:\Microservice\jdk\jdk17\bin\java.exe"" -Dfile.encoding=UTF-8 -jar target\explore-platform-1.0.0.jar", 0, False
        WaitPort 8080, 40
    End If
End If

' ---- 2. Frontend ----
If Not CheckPort(5173) Then
    sh.CurrentDirectory = dir & "\frontend"
    sh.Run """D:\Microservice\node.js\node.js-22.22.3\node.exe"" node_modules\vite\bin\vite.js", 0, False
    WaitPort 5173, 30
End If

' ---- 3. Browser ----
sh.Run "http://localhost:5173"

' ============ Functions ============
Function CheckPort(port)
    tf = sh.ExpandEnvironmentStrings("%TEMP%") & "\portcheck.txt"
    sh.Run "cmd /c (netstat -ano | findstr "":" & port & """ | findstr ""LISTENING"") >""" & tf & """ 2>nul", 0, True
    CheckPort = False
    If fso.FileExists(tf) Then
        If fso.GetFile(tf).Size > 0 Then CheckPort = True
        fso.DeleteFile tf
    End If
End Function

Sub WaitPort(port, sec)
    elapsed = 0
    Do While Not CheckPort(port) And elapsed < sec
        WScript.Sleep 2000
        elapsed = elapsed + 2
    Loop
End Sub
