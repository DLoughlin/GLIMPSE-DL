@echo off
setlocal EnableDelayedExpansion
set "ROOT=C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface"
set "CP=%ROOT%\src\MIEnhancements;%ROOT%\src\mif411"
for %%f in ("%ROOT%\lib\*.jar") do set "CP=!CP!;%%~ff"
for /R "%ROOT%\lib\geotools-28.6" %%f in (*.jar) do set "CP=!CP!;%%~ff"
if not exist "%ROOT%\build_tmp\clipboard-smoke" mkdir "%ROOT%\build_tmp\clipboard-smoke"
javac -cp "!CP!" -sourcepath "%ROOT%\src\MIEnhancements;%ROOT%\src\mif411" -d "%ROOT%\build_tmp\clipboard-smoke" "%ROOT%\src\MIEnhancements\graphDisplay\AbstractMapPanel.java"
exit /b %ERRORLEVEL%
