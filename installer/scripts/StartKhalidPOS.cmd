@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not exist "%~dp0jre\bin\javaw.exe" (
  echo Khalid POS is missing a required file. Please run the installer again.
  pause
  exit /b 1
)
if not exist "%~dp0app\PointOfSale.jar" (
  echo Khalid POS is missing a required file. Please run the installer again.
  pause
  exit /b 1
)

sc.exe query KhalidPOS_MariaDB | findstr /C:"RUNNING" >nul
if errorlevel 1 (
  net start KhalidPOS_MariaDB >nul 2>&1
)

set /a waited=0
:wait_db
"%~dp0mariadb\bin\mysqladmin.exe" --protocol=TCP --host=127.0.0.1 --port=3307 --user=root ping >nul 2>&1
if not errorlevel 1 goto launch

set /a waited+=1
if %waited% GEQ 30 goto not_ready

if %waited%==1 net start KhalidPOS_MariaDB >nul 2>&1
if %waited%==3 net start KhalidPOS_MariaDB >nul 2>&1
if %waited%==6 net start KhalidPOS_MariaDB >nul 2>&1
if %waited%==12 net start KhalidPOS_MariaDB >nul 2>&1
if %waited%==21 net start KhalidPOS_MariaDB >nul 2>&1

ping -n 2 127.0.0.1 >nul
goto wait_db

:not_ready
echo The shop records are not ready yet. Please wait a moment and try again.
echo If this keeps happening, restart the computer.
pause
exit /b 1

:launch
cd /d "%~dp0app"
if exist "%~dp0app\lib\" (
  start "" "%~dp0jre\bin\javaw.exe" -cp "%~dp0app\PointOfSale.jar;%~dp0app\lib\*" view.Form_Login_old
) else (
  start "" "%~dp0jre\bin\javaw.exe" -jar "%~dp0app\PointOfSale.jar"
)
exit /b 0
