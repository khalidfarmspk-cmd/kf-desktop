' Creates a "Khalid POS" shortcut on the current user's Desktop.
' Double-click this file from inside the project folder on any machine —
' it works out its own location, so the folder can be named anything
' (PointOfSale-Desktop, kf-desktop, ...) and sit anywhere.

Option Explicit

Dim sh, fso, root, vbs, ico, lnkPath, lnk
Set sh  = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

root = fso.GetParentFolderName(WScript.ScriptFullName)
vbs  = root & "\Start-KhalidPOS.vbs"
ico  = root & "\khalidpos.ico"

If Not fso.FileExists(vbs) Then
  MsgBox "Start-KhalidPOS.vbs was not found next to this script." & vbCrLf & _
         "Copy this file into the shop software folder and run it there.", _
         vbCritical, "Khalid POS"
  WScript.Quit 1
End If

lnkPath = sh.SpecialFolders("Desktop") & "\Khalid POS.lnk"

Set lnk = sh.CreateShortcut(lnkPath)
lnk.TargetPath       = sh.ExpandEnvironmentStrings("%WINDIR%") & "\System32\wscript.exe"
lnk.Arguments        = """" & vbs & """"
lnk.WorkingDirectory = root & "\app"
lnk.Description      = "Khalid Farms POS"
If fso.FileExists(ico) Then
  lnk.IconLocation = ico & ",0"
End If
lnk.Save

MsgBox "Shortcut created on the Desktop." & vbCrLf & vbCrLf & _
       "Runs: " & vbs, vbInformation, "Khalid POS"
