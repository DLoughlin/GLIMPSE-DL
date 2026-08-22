rem If the following command does not start the ScenarioBuilder, correct the JAVA_HOME path
rem Migration inventory note: this launcher currently assumes a Java 8 Corretto JRE layout
rem and starts ScenarioBuilder by classpath/jar launch (not Java module-path launch).

set JAVA_HOME=%~dp0\amazon-corretto-8.442.06.1-windows-x64-jre

if not exist "%JAVA_HOME%"\bin\java.exe (
  echo JAVA_HOME setting needs to be fixed
  pause
  GOTO END
) 

set JAVA_JVM_PATH=%JAVA_HOME%\bin\server

set PATH=.;%JAVA_JVM_PATH%;%JAVA_HOME%;%JAVA_HOME%\bin;..\..\ModelInterface;%PATH%

java -Djava.util.logging.config.file -Dprism.order=sw -jar .\GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar -options options_GCAM-USA-8.2-windows.txt

)
:END