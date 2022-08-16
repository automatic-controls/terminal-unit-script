@echo off
echo.
set "jdk=C:\Program Files\Java\jdk-17.0.2\bin"
set "classes=%~dp0classes"
set "mainClass=TerminalUnitTest"
if exist "%classes%" rmdir /S /Q "%classes%" >nul
if exist "%~dp0%mainClass%.jar" del /F "%~dp0%mainClass%.jar" >nul
mkdir "%classes%" >nul
"%jdk%\javac.exe" --release 8 -d "%classes%" --class-path "%~dp0src;C:\Users\cvogt\Desktop\Repositories\commissioning-scripts\CommissioningScripts.jar;C:\Users\cvogt\Desktop\Repositories\addon-dev-script\lib\*;C:\Users\cvogt\Desktop\Repositories\commissioning-scripts\lib\*" "%~dp0src\%mainClass%.java"
if %ERRORLEVEL% NEQ 0 (
  rmdir /S /Q "%classes%" >nul
  echo.
  echo Press any key to exit.
  pause >nul
  exit
)
"%jdk%\jar.exe" --create --verbose --file "%~dp0%mainClass%.jar" --main-class "%mainClass%" -C "%classes%" . >nul
if %ERRORLEVEL% NEQ 0 (
  rmdir /S /Q "%classes%" >nul
  echo.
  echo Press any key to exit.
  pause >nul
  exit
)
rmdir /S /Q "%classes%" >nul
exit