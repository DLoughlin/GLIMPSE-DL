@echo off
rem Launch ScenarioBuilder with JavaFX module-path flags for Java 21+

set "SCRIPT_DIR=%~dp0"
set "JAVA_EXE="

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if not defined JAVA_EXE if exist "%SCRIPT_DIR%amazon-corretto-8.442.06.1-windows-x64-jre\bin\java.exe" (
  set "JAVA_HOME=%SCRIPT_DIR%amazon-corretto-8.442.06.1-windows-x64-jre"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVA_EXE for %%I in (java.exe) do set "JAVA_EXE=%%~$PATH:I"

if not defined JAVA_EXE (
  echo Could not find java.exe. Set JAVA_HOME to a Java 21+ installation or add java.exe to PATH.
  pause
  goto END
)

set "JAVAFX_LIB=%SCRIPT_DIR%GLIMPSE-ScenarioBuilder\libs\javafx-21\win"
if not exist "%JAVAFX_LIB%\javafx-controls-21.0.4-win.jar" (
  echo JavaFX runtime jars not found in "%JAVAFX_LIB%".
  echo Expected javafx-21/win jars under GLIMPSE-ScenarioBuilder\libs.
  pause
  goto END
)

set "PATH=.;%JAVA_HOME%\bin\server;%JAVA_HOME%\bin;..\..\ModelInterface;%PATH%"

"%JAVA_EXE%" -Dprism.order=sw --module-path "%JAVAFX_LIB%" --add-modules=javafx.controls,javafx.fxml --add-exports=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED -jar .\GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar -options options_GCAM-global-8.2-windows.txt

:END