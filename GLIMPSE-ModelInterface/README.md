# GLIMPSE ModelInterface

## Runtime baseline (pre Java 21 migration)

- Windows launcher: `run_GLIMPSE-ModelInterface-Windows.bat`
  - Requires `%JAVA_HOME%\\bin\\java.exe`.
  - Prepends `%JAVA_HOME%\\bin\\server` and `%JAVA_HOME%\\bin` to `PATH`.
  - Launches with `java -jar ./GLIMPSE-ModelInterface.jar ...`.
- Linux launcher: `run-GLIMPSE-ModelInterface_Linux.sh`
  - Requires `$JAVA_HOME/bin/java`.
  - Prepends `$JAVA_HOME/bin/server` and `$JAVA_HOME/bin` to `PATH`.
  - Launches with `java -jar ./GLIMPSE-ModelInterface.jar ...`.
- Current `.classpath` in this module relies on `JRE_CONTAINER` and third-party libraries; it does not declare JavaFX module jars explicitly.

## Command-line font size override (`-s`)

You can override the General UI font size at startup with:

- `-s <font size>`

If provided with an allowed value, this overrides the `fontSize` value from `model_interface.properties` for that launch and is persisted back to the properties file.

Allowed values (must match Preferences -> General -> Font size):

- `8`, `9`, `10`, `11`, `12`, `13`, `14`, `15`, `16`, `18`, `20`, `22`, `24`

### Windows (`cmd.exe`) example

```bat
set FONT_SIZE=14
start java -jar .\GLIMPSE-ModelInterface.jar -q %QUERY_FILE% -o %DATABASE% -u %UNITS% -f %FAVORITES% -p %REGIONS% -m %MAPS% -s %FONT_SIZE%
```

### Linux (`bash`) example

```bash
FONT_SIZE="14"
java -jar ./GLIMPSE-ModelInterface.jar -q "$QUERY_FILE" -o "$DATABASE" -u "$UNITS" -f "$FAVORITES" -p "$REGIONS" -m "$MAPS" -s "$FONT_SIZE"
```
