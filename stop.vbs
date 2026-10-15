' Explore Platform - Silent Stopper
' Closes Backend (8080) + Frontend (5173), keeps RabbitMQ running

Set sh = CreateObject("WScript.Shell")

KillPort 8080
KillPort 5173

Sub KillPort(port)
    tf = sh.ExpandEnvironmentStrings("%TEMP%") & "\killport.txt"
    sh.Run "cmd /c (for /f ""tokens=5"" %a in ('netstat -ano ^| findstr "":" & port & """ ^| findstr ""LISTENING""') do taskkill /PID %a /F) >""" & tf & """ 2>nul", 0, True
    If CreateObject("Scripting.FileSystemObject").FileExists(tf) Then
        CreateObject("Scripting.FileSystemObject").DeleteFile tf
    End If
End Sub
