' Khalid POS — double-click launcher for this PC.
' Starts XAMPP MySQL on port 3307 if needed, then opens the till.

Option Explicit

Const WAIT_SECONDS = 30

Dim sh, fso, root, appDir, libDir, classesDir, javaw
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
root = fso.GetParentFolderName(WScript.ScriptFullName)
appDir = root & "\app"
libDir = root & "\library"
classesDir = appDir & "\build\classes"

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

Function PortReady()
  PortReady = (RunHidden("netstat -ano -p TCP | findstr /C:"":3307"" | findstr /I LISTENING >nul") = 0)
End Function

Function FindJavaw()
  Dim p, i, c
  p = Array( _
    "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot\bin\javaw.exe", _
    "C:\Program Files\Java\jdk1.8.0_202\bin\javaw.exe", _
    "C:\Program Files\Java\jre1.8.0_202\bin\javaw.exe" _
  )
  For i = 0 To UBound(p)
    If fso.FileExists(p(i)) Then
      FindJavaw = p(i)
      Exit Function
    End If
  Next
  c = sh.ExpandEnvironmentStrings("%JAVA_HOME%\bin\javaw.exe")
  If fso.FileExists(c) Then
    FindJavaw = c
    Exit Function
  End If
  FindJavaw = "javaw.exe"
End Function

If Not fso.FolderExists(classesDir) Then
  Fail "The shop software files were not found. Open this shortcut from the PointOfSale-Desktop folder."
End If
If Not fso.FileExists(libDir & "\mysql-connector-java-5.1.49.jar") Then
  Fail "The shop software files were not found. Open this shortcut from the PointOfSale-Desktop folder."
End If

If Not PortReady() Then
  If fso.FileExists("C:\xampp\mysql_start.bat") Then
    sh.Run Quote("C:\xampp\mysql_start.bat"), 0, False
  ElseIf fso.FileExists("C:\xampp\mysql\bin\mysqld.exe") Then
    sh.Run Quote("C:\xampp\mysql\bin\mysqld.exe") & " --defaults-file=" & Quote("C:\xampp\mysql\bin\my.ini"), 0, False
  End If
End If

Dim i, ready
ready = False
For i = 1 To WAIT_SECONDS
  If PortReady() Then
    ready = True
    Exit For
  End If
  WScript.Sleep 1000
Next

If Not ready Then
  Fail "The shop records are not ready. Open XAMPP and start MySQL, then try again."
End If

javaw = FindJavaw()
If javaw <> "javaw.exe" Then
  If Not fso.FileExists(javaw) Then Fail "Java was not found on this computer."
End If

sh.CurrentDirectory = appDir
sh.Run Quote(javaw) & " -cp " & Quote(classesDir & ";" & libDir & "\*") & " view.Form_Login_old", 1, False
