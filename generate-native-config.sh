#!/bin/bash

set -e

echo "======================================"
echo "Generating Native Image Configuration"
echo "======================================"
echo ""

CONFIG_DIR="src/main/resources/META-INF/native-image/com.sshman/sshman"
JAR_FILE="target/sshman-0.1.1-jar-with-dependencies.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Building project first..."
    mvn clean package
    echo ""
fi

echo "Running SSHman with tracing agent to capture reflection usage..."
echo ""

# Create a temporary directory for generated config
TEMP_CONFIG_DIR=$(mktemp -d)
echo "Temporary config directory: $TEMP_CONFIG_DIR"
echo ""

# Run various commands to exercise the application
commands=(
    "--help"
    "--version"
    "generate --help"
    "list --help"
    "info --help"
    "use --help"
    "init --help"
    "connect --help"
    "connect-new --help"
    "archive --help"
    "unarchive --help"
    "rotate --help"
)

for cmd in "${commands[@]}"; do
    echo "Running: sshman $cmd"
    java -agentlib:native-image-agent=config-merge-dir="$TEMP_CONFIG_DIR" \
         -jar "$JAR_FILE" $cmd 2>/dev/null || true
done

echo ""
echo "======================================"
echo "Generated configuration files:"
echo "======================================"
ls -lh "$TEMP_CONFIG_DIR"
echo ""

echo "Review the generated files in: $TEMP_CONFIG_DIR"
echo ""
echo "To use this configuration, either:"
echo "  1. Copy files to $CONFIG_DIR (overwrites existing)"
echo "  2. Manually merge with existing configuration"
echo ""
echo "Copy command:"
echo "  cp $TEMP_CONFIG_DIR/*.json $CONFIG_DIR/"
echo ""
