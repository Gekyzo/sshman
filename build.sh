#!/bin/bash
set -e

echo "Building sshman..."
mvn clean package -q

JAR_FILE=$(find target -name "sshman-*-jar-with-dependencies.jar" | head -1)

if [[ -f "$JAR_FILE" ]]; then
    echo "Build successful!"
    echo ""
    echo "Run with:"
    echo "  java -jar $JAR_FILE"
else
    echo "Build failed - JAR not found"
    exit 1
fi
