# GLIMPSE ModelInterface

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
