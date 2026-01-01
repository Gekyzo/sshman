# Building SSHman as a GraalVM Native Image

This guide explains how to compile SSHman into a native executable using GraalVM native-image. This allows you to distribute SSHman as a standalone binary without requiring Java to be installed on the target system.

## Benefits

- **No Java Required**: Distribute a single executable that runs without a JRE
- **Fast Startup**: ~10-50ms startup time (vs ~500-1000ms with JVM)
- **Low Memory**: ~10-30 MB memory usage (vs ~100-200 MB with JVM)
- **Easy Distribution**: Single binary file instead of JAR + JRE

## Prerequisites

### 1. Install GraalVM

Using SDKMAN (recommended):

```bash
# Install SDKMAN if not already installed
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install GraalVM
sdk install java 21.0.1-graal

# Set as default (or use temporarily)
sdk use java 21.0.1-graal

# Verify installation
java -version
# Should show: openjdk version "21.0.1" ... GraalVM ...
```

Manual installation:
- Download GraalVM from [graalvm.org](https://www.graalvm.org/downloads/)
- Set `JAVA_HOME` and add to `PATH`

### 2. Install Native Image Tool

```bash
gu install native-image

# Verify installation
native-image --version
```

## Building the Native Image

### Quick Build

Use the provided build script:

```bash
./build-native.sh
```

This will:
1. Clean previous builds
2. Compile the project with Maven
3. Create a native executable at `target/sshman`

### Manual Build

```bash
# Build with Maven using the native profile
mvn -Pnative clean package

# Or build with native-image directly
mvn clean package
native-image -jar target/sshman-0.1.1-jar-with-dependencies.jar \
  --no-fallback \
  -H:+ReportExceptionStackTraces \
  --initialize-at-build-time=org.slf4j \
  --initialize-at-build-time=ch.qos.logback \
  -o sshman
```

## Testing the Native Image

```bash
# Test basic commands
./target/sshman --version
./target/sshman --help
./target/sshman list

# Run all tests
./target/sshman generate --help
./target/sshman connect --help
```

## Configuration Files

Native image configuration is located in:
```
src/main/resources/META-INF/native-image/com.sshman/sshman/
├── reflect-config.json       # Reflection configuration
├── resource-config.json      # Resource files to include
└── native-image.properties   # Build options
```

### Updating Configuration

If you add new commands or modify reflection usage, you may need to update the configuration:

#### Option 1: Using the Tracing Agent (Recommended)

```bash
./generate-native-config.sh
```

This runs SSHman with various commands and captures all reflection usage. Review and merge the generated configuration.

#### Option 2: Manual Configuration

Edit `reflect-config.json` to add new classes that use reflection:

```json
{
  "name": "com.sshman.NewCommand",
  "allDeclaredFields": true,
  "allDeclaredConstructors": true,
  "allDeclaredMethods": true
}
```

## Troubleshooting

### Build Fails with ClassNotFoundException

Add the missing class to `reflect-config.json`:

```json
{
  "name": "com.example.MissingClass",
  "allDeclaredConstructors": true,
  "allDeclaredMethods": true
}
```

### Build Fails with "Resource not found"

Add the resource to `resource-config.json`:

```json
{
  "resources": {
    "includes": [
      {"pattern": "your-resource\\.txt"}
    ]
  }
}
```

### Runtime ClassNotFoundException or MethodNotFoundException

You likely need to add reflection configuration. Run the tracing agent to capture the missing configuration:

```bash
java -agentlib:native-image-agent=config-output-dir=/tmp/config \
     -jar target/sshman-0.1.1-jar-with-dependencies.jar <your-command>

# Review generated files in /tmp/config
# Merge with existing configuration
```

### Binary Size Too Large

The native image will be 20-40 MB. To reduce size:

1. Remove unnecessary dependencies
2. Use `-Ob` (optimize for size) instead of default `-O2`:
   ```bash
   native-image -Ob -jar target/sshman-0.1.1.jar
   ```
3. Strip debug symbols (Linux/macOS):
   ```bash
   strip target/sshman
   ```

## Distribution

After building, distribute the native executable:

```bash
# Copy the binary
cp target/sshman /usr/local/bin/sshman

# Or create a release package
tar -czf sshman-linux-amd64.tar.gz -C target sshman
```

### Platform-Specific Builds

Native images are platform-specific. Build on each target platform:

- **Linux**: Build on Linux (produces ELF binary)
- **macOS**: Build on macOS (produces Mach-O binary)
- **Windows**: Build on Windows (produces .exe)

For cross-platform support, use CI/CD (GitHub Actions, GitLab CI) to build for multiple platforms.

## Performance Comparison

| Metric | JVM (JAR) | Native Image |
|--------|-----------|--------------|
| Startup Time | ~500-1000ms | ~10-50ms |
| Memory Usage | ~100-200 MB | ~10-30 MB |
| Binary Size | ~15 MB + JRE | ~20-40 MB |
| Java Required | Yes | No |

## Advanced Configuration

### Custom Build Options

Edit `native-image.properties` or add to `pom.xml`:

```properties
Args = --no-fallback \
       -H:+ReportExceptionStackTraces \
       -march=native \
       --gc=serial
```

### Initialize Classes at Build Time

For faster startup, initialize more classes at build time:

```bash
--initialize-at-build-time=com.sshman
```

⚠️ **Warning**: Only initialize classes that don't depend on runtime state.

## References

- [GraalVM Native Image Documentation](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Native Image Maven Plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)
- [Picocli Native Image Guide](https://picocli.info/picocli-on-graalvm.html)
