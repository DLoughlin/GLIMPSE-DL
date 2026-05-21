# GLIMPSE Launch4j Packaging

This folder contains Launch4j configs to package GLIMPSE as double-click `.exe` launchers.

## What You Get

- `GLIMPSE-USA-8.2.exe`
- `GLIMPSE-global-8.2.exe`

Both launchers are configured to:

- Run `GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar`
- Pass the matching options file (`USA` or `global`)
- Use bundled Java 8 runtime at `amazon-corretto-8.442.06.1-windows-x64-jre`

## Build Steps

1. Install Launch4j.
2. Set `LAUNCH4J_HOME` to that install directory.
3. Run the build script from anywhere:

```bat
C:\Users\danlo\git\GLIMPSE-CE\auxiliary\launch4j\build-launch4j-exes.bat
```

## Manual Launch4j Command (optional)

```bat
"%LAUNCH4J_HOME%\launch4jc.exe" "C:\Users\danlo\git\GLIMPSE-CE\auxiliary\launch4j\GLIMPSE-USA-8.2-launch4j.xml"
"%LAUNCH4J_HOME%\launch4jc.exe" "C:\Users\danlo\git\GLIMPSE-CE\auxiliary\launch4j\GLIMPSE-global-8.2-launch4j.xml"
```

## Notes

- If the bundled runtime folder is missing, the EXE will fail to launch.
- If you need software rendering for VM compatibility, you can add `-Dprism.order=sw` in the `<cmdLine>` element of the XML config.
