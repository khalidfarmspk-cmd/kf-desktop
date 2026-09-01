' Khalid POS launcher — no console window.
' Starts the shop-records service if needed, waits up to 30 seconds, then opens the till.

Option Explicit

Const SERVICE_NAME = "KhalidPOS_MariaDB"
Const WAIT_SECONDS = 30

Dim sh, fso, appDir
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
appDir = fso.GetParentFolderName(WScript.ScriptFullName)

Sub Fail(msg)
  MsgBox msg, vbCritical, "Khalid POS"
  WScript.Quit 1
End Sub

Function Quote(s)
  Quote = """" & s & """"
End Function

Function RunHidden(cmd)
  RunHidden = sh.Run("%comspec% /c " & cmd, 0, True)
End Function

Function ServiceRunning()
  ServiceRunning = (RunHidden("sc.exe query " & SERVICE_NAME & " | findstr /C:""RUNNING"" >nul") = 0)
End Function

Function DatabaseReady()
  Dim exe
  exe = Quote(appDir & "\mariadb\bin\mysqladmin.exe")
  DatabaseReady = (RunHidden(exe & " --protocol=TCP --host=127.0.0.1 --port=3307 --user=root ping >nul 2>&1") = 0)
End Function

If Not fso.FileExists(appDir & "\jre\bin\javaw.exe") Then
  Fail "Khalid POS is missing a required file. Please run the installer again."
End If

If Not fso.FileExists(appDir & "\app\PointOfSale.jar") Then
  Fail "Khalid POS is missing a required file. Please run the installer again."
End If

If Not ServiceRunning() Then
  RunHidden "net start " & SERVICE_NAME & " >nul 2>&1"
End If

Dim i, ready
ready = False
For i = 1 To WAIT_SECONDS
  If DatabaseReady() Then
    ready = True
    Exit For
  End If
  If (i = 1) Or ((i Mod 3) = 0) Then
    If Not ServiceRunning() Then
      RunHidden "net start " & SERVICE_NAME & " >nul 2>&1"
    End If
  End If
  WScript.Sleep 1000
Next

If Not ready Then
  Fail "The shop records are not ready yet. Please wait a moment and try again. If this keeps happening, restart the computer."
End If

Dim javaw, jar, workDir, cmd
javaw = Quote(appDir & "\jre\bin\javaw.exe")
jar = Quote(appDir & "\app\PointOfSale.jar")
workDir = appDir & "\app"
sh.CurrentDirectory = workDir

If fso.FolderExists(workDir & "\lib") Then
  cmd = javaw & " -cp " & Quote(workDir & "\PointOfSale.jar;" & workDir & "\lib\*") & " view.Form_Login_old"
Else
  cmd = javaw & " -jar " & jar
End If

sh.Run cmd, 1, False
