@echo off
setlocal EnableDelayedExpansion
set CP=.
for %%I in ("C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface\lib\*.jar") do set CP=!CP!;%%~fI
javac -cp "!CP!;C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface\src\mif411" -sourcepath "C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface\src\mif411" -d "C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface\build_tmp\compile-smoke" "C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ModelInterface\src\mif411\ModelInterface\ModelGUI2\DbViewer.java"
exit /b %errorlevel%
