#!/bin/bash
# Linux Java 21 migration: prefer configured JAVA_HOME, then bundled Corretto, then PATH java.

JAVA_BIN=""
LEGACY_BUNDLED_JAVA="./amazon-corretto-8.462.08.1-linux-x64/bin/java"
JAVAFX_DIR="./GLIMPSE-ScenarioBuilder/libs/javafx-21/linux"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_BIN="$JAVA_HOME/bin/java"
elif [ -x "$LEGACY_BUNDLED_JAVA" ]; then
  JAVA_BIN="$LEGACY_BUNDLED_JAVA"
elif command -v java >/dev/null 2>&1; then
  JAVA_BIN="$(command -v java)"
else
  echo "Could not find java. Set JAVA_HOME or add java to PATH."
  exit 1
fi

if [ ! -f "$JAVAFX_DIR/javafx-controls-21.0.4-linux.jar" ]; then
  echo "Missing JavaFX runtime jars in $JAVAFX_DIR"
  exit 1
fi

"$JAVA_BIN" -Dprism.order=sw \
  --module-path "$JAVAFX_DIR" \
  --add-modules javafx.controls,javafx.fxml \
  --add-exports=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED \
  --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
  -jar ./GLIMPSE-ScenarioBuilder/GLIMPSE-ScenarioBuilder.jar \
  -options options_GCAM-USA-8.2-linux.txt

exit 0
