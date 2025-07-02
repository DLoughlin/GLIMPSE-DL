rem If the following command does not start the ScenarioBuilder, correct the JAVA_HOME path

set JAVA_HOME=%~dp0\amazon-corretto-8.442.06.1-windows-x64-jre

if not exist "%JAVA_HOME%"\bin\java.exe (
  echo JAVA_HOME setting needs to be fixed
  pause
  GOTO END
) 

set JAVA_JVM_PATH=%JAVA_HOME%\bin\server

set PATH=.;%JAVA_JVM_PATH%;%JAVA_HOME%;%JAVA_HOME%\bin;..\..\ModelInterface;%PATH%

java -Dprism.order=sw -jar .\GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar -options options_GCAM-global-8.2.txt

)
:END