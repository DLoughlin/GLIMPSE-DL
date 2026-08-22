#!/bin/bash
# If the following command does not start the ModelInterface, correct the JAVA_HOME path
# Migration inventory note: this launcher expects JAVA_HOME/bin/java and JAVA_HOME/bin/server
# and uses java -jar (not module-path JavaFX launch flags).

# Set these variables
#JAVA_HOME="../amazon-corretto-8.462.08.1-linux-x64"
QUERY_FILE="./config/Main_queries_GLIMPSE-8.2.xml"
DATABASE="../../GCAM-Model/gcam-v8.2/output/database"
UNITS="./config/units_rules.csv"
FAVORITES="./config/favorite_queries_list.txt"
REGIONS="./config/preset_region_list.txt"
MAPS="./map_resources"
# Optional UI font size override (must match Preferences > General allowed values)
# Example: FONT_SIZE="14"

# Resolve java using the same migration order used by ScenarioBuilder launchers.
JAVA_BIN=""
LEGACY_BUNDLED_JAVA="../amazon-corretto-8.462.08.1-linux-x64/bin/java"

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

# To pass font size at launch, append: -s "$FONT_SIZE"
"$JAVA_BIN" -jar ./GLIMPSE-ModelInterface.jar -q "$QUERY_FILE" -o "$DATABASE" -u "$UNITS" -f "$FAVORITES" -p "$REGIONS" -m "$MAPS"

exit 0
