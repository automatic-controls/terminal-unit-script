@echo off
setlocal EnableDelayedExpansion
echo.

:: Modify these parameters when necessary
set "WebCTRL=C:\WebCTRL8.0"
set "mainClass=TerminalUnitTest"
set "package=aces\webctrl\scripts\terminalunits\"
set "jdk=%JAVA_HOME%\bin"
set "release=8"

if not exist "%~dp0lib" mkdir "%~dp0lib" >nul 2>nul
call :collect "%~dp0DEPENDENCIES" "%~dp0lib"
if %ERRORLEVEL% NEQ 0 (
  echo Failed to collect necessary dependencies.
  echo Press any key to exit.
  pause >nul
  exit
)
set "classes=%~dp0classes"
if exist "%classes%" rmdir /S /Q "%classes%" >nul
if exist "%~dp0%mainClass%.jar" del /F "%~dp0%mainClass%.jar" >nul
mkdir "%classes%" >nul
echo Compiling...
"%jdk%\javac.exe" --release %release% -d "%classes%" --class-path "%~dp0src;%~dp0lib\*" "%~dp0src\%package%%mainClass%.java"
if %ERRORLEVEL% NEQ 0 (
  rmdir /S /Q "%classes%" >nul
  echo.
  echo Compilation failed.
  echo Press any key to exit.
  pause >nul
  exit
)
echo Packing...
robocopy /E "%~dp0src" "%classes%" /XF "*.java" >nul 2>nul
if exist "%~dp0%mainClass%.jar" del /F "%~dp0%mainClass%.jar" >nul 2>nul
"%jdk%\jar.exe" --create --verbose --file "%~dp0%mainClass%.jar" --main-class "%package:\=.%%mainClass%" -C "%classes%" . >nul
if %ERRORLEVEL% NEQ 0 (
  rmdir /S /Q "%classes%" >nul
  echo.
  echo Jar creation failed.
  echo Press any key to exit.
  pause >nul
  exit
)
rmdir /S /Q "%classes%" >nul
echo Copying to CommissioningScripts repository...
copy /y "%~dp0%mainClass%.jar" "%~dp0..\commissioning-scripts\src\aces\webctrl\scripts\commissioning\fixed\%mainClass%.jar" >nul
if %ERRORLEVEL% NEQ 0 (
  echo.
  echo Copy task failed.
  echo Press any key to exit.
  pause >nul
  exit
)
echo Build successful.
echo Press any key to exit.
pause >nul
exit


:: Collect dependencies from the WebCTRL installation or from external websites
:: Parameters: <dependency-file> <output-folder>
:collect
  setlocal
    set "tmp1=%~dp0tmp1"
    set "tmp2=%~dp0tmp2"
    set "err=0"
    set "msg=0"
    dir "%~f2\*.jar" /B /A-D 2>nul >"%tmp1%"
    for /F "usebackq tokens=1,2,* delims=:" %%i in ("%~f1") do (
      set "findString=%%j"
      if "!findString:~-4!" NEQ ".jar" set "findString=%%j-[0-9].*"
      set "exists=0"
      for /F %%a in ('findstr /R /X "!findString!" "%tmp1%"') do (
        set "exists=1"
      )
      if "!exists!" EQU "0" (
        set "msg=1"
        if /I "%%i" EQU "url" (
          curl --location --fail --silent --output-dir "%~f2" --remote-name %%k
          if !ErrorLevel! EQU 0 (
            echo Collected: %%j
          ) else (
            set "err=1"
            echo Failed to collect: %%j
          )
        ) else if /I "%%i" EQU "file" (
          set "file="
          dir "%WebCTRL%\%%k\*.jar" /B /A-D 2>nul >"%tmp2%"
          for /F %%a in ('findstr /R /X "!findString!" "%tmp2%"') do (
            set "file=%%a"
          )
          if "!file!" EQU "" (
            set "err=1"
            echo Failed to collect: %%j
          ) else (
            copy /Y "%WebCTRL%\%%k\!file!" "%~f2\!file!" >nul
            if !ErrorLevel!==0 (
              echo Collected: %%j
            ) else (
              set "err=1"
              echo Failed to collect: %%j
            )
          )
        ) else (
          set "err=1"
          echo Failed to collect: %%j
        )
      )
    )
    if "%msg%" EQU "1" echo.
    if exist "%tmp1%" del /F "%tmp1%" >nul 2>nul
    if exist "%tmp2%" del /F "%tmp2%" >nul 2>nul
  endlocal & exit /b %err%