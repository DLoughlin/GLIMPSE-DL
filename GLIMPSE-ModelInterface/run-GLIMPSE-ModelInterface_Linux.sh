#!/bin/bash
# If the following command does not start the ModelInterface, correct the JAVA_HOME path

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

# Checking JAVA_HOME setup
if [ ! -f "$JAVA_HOME/bin/java" ]; then
  echo "JAVA_HOME setting needs to be fixed"
  read -p "Press enter to continue..."
  exit 1
fi

JAVA_JVM_PATH="$JAVA_HOME/bin/server"
export PATH=".:$JAVA_JVM_PATH:$JAVA_HOME:$JAVA_HOME/bin:$PATH"

# To pass font size at launch, append: -s "$FONT_SIZE"
java -jar ./GLIMPSE-ModelInterface.jar -q $QUERY_FILE -o $DATABASE -u $UNITS -f $FAVORITES -p $REGIONS -m $MAPS

exit 0
