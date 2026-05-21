@echo off
setlocal

REM Build GLIMPSE USA/global launchers with Launch4j.
REM
REM Prereqs:
REM   1) Launch4j installed
REM   2) LAUNCH4J_HOME points to Launch4j install folder
REM      Example: set LAUNCH4J_HOME=C:\tools\launch4j

set LAUNCH4J_HOME=C:\tools\launch4j

if "%LAUNCH4J_HOME%"=="" (
  echo ERROR: LAUNCH4J_HOME is not set.
  echo Set it first, e.g.:
  echo   set LAUNCH4J_HOME=C:\tools\launch4j
  exit /b 1
)

if not exist "%LAUNCH4J_HOME%\launch4jc.exe" (
  echo ERROR: launch4jc.exe not found at:
  echo   %LAUNCH4J_HOME%\launch4jc.exe
  exit /b 1
)

set "ROOT=%~dp0..\.."
pushd "%ROOT%"

echo Building GLIMPSE-USA-8.2.exe ...
"%LAUNCH4J_HOME%\launch4jc.exe" "%ROOT%\auxiliary\launch4j\GLIMPSE-USA-8.2-launch4j.xml" || goto :FAIL

echo Building GLIMPSE-global-8.2.exe ...
"%LAUNCH4J_HOME%\launch4jc.exe" "%ROOT%\auxiliary\launch4j\GLIMPSE-global-8.2-launch4j.xml" || goto :FAIL

echo.
echo Success. Created:
echo   %ROOT%\GLIMPSE-USA-8.2.exe
echo   %ROOT%\GLIMPSE-global-8.2.exe
popd
exit /b 0

:FAIL
echo.
echo ERROR: Launch4j build failed.
popd
exit /b 1
