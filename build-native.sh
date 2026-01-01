#!/bin/bash

set -e

echo "======================================"
echo "Building SSHman Native Image"
echo "======================================"
echo ""

# Check if GraalVM is installed
if ! command -v native-image &> /dev/null; then
    echo "❌ Error: native-image not found in PATH"
    echo ""
    echo "Please install GraalVM and the native-image tool:"
    echo "  1. Install GraalVM: sdk install java 21.0.1-graal"
    echo "  2. Use GraalVM: sdk use java 21.0.1-graal"
    echo "  3. Install native-image: gu install native-image"
    echo ""
    exit 1
fi

# Check Java version
echo "Java version:"
java -version
echo ""

# Build the project and create native image
echo "Building native image with Maven..."
echo ""

mvn -Pnative clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "======================================"
    echo "✓ Build successful!"
    echo "======================================"
    echo ""
    echo "Native executable created at: target/sshman"
    echo ""
    echo "Binary size:"
    ls -lh target/sshman | awk '{print "  " $5}'
    echo ""
    echo "Test the binary:"
    echo "  ./target/sshman --version"
    echo "  ./target/sshman --help"
    echo ""
else
    echo ""
    echo "======================================"
    echo "❌ Build failed!"
    echo "======================================"
    echo ""
    exit 1
fi
