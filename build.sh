#!/bin/bash
set -e

echo "Building sshman..."
mvn clean package -q

JAR_FILE="target/sshman-0.1.0-jar-with-dependencies.jar"

if [[ -f "$JAR_FILE" ]]; then
    echo "Build successful!"
    echo ""
    echo "Run with:"
    echo "  java -jar $JAR_FILE"
    echo "  ./sshman  (if wrapper exists)"
else
    echo "Build failed - JAR not found"
    exit 1
fi
