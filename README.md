# sshman

> A modern, intuitive CLI tool for managing SSH keys and connections

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**sshman** simplifies SSH key management by providing a centralized, user-friendly command-line interface. Generate, list, and inspect SSH keys with ease, featuring security checks, detailed metadata, and beautiful output formatting.

## ✨ Features

- 🔑 **Generate SSH Keys** - Create ED25519, RSA, or ECDSA keys with sensible defaults
- 📋 **List Keys** - View all SSH keys with detailed information (type, permissions, modification dates)
- 🔍 **Inspect Keys** - Display comprehensive key information including fingerprints and security warnings
- 🛡️ **Security Checks** - Detect insecure file permissions and weak key types
- 🎨 **Beautiful Output** - Color-coded, well-formatted terminal output
- ⚡ **Fast & Lightweight** - Single JAR executable with no runtime dependencies

## 📦 Installation

### Prerequisites

- **Java 17 or higher** - [Download Java](https://adoptium.net/)
- **Maven 3.6+** - For building from source
- **ssh-keygen** - Should be pre-installed on most Unix-like systems

### Build from Source

```bash
# Clone the repository
git clone https://github.com/Gekyzo/sshman.git
cd sshman

# Build the project
mvn clean package

# The executable JAR will be created at:
# target/sshman-0.1.0-jar-with-dependencies.jar

# Test the build
./sshman --version
```

### System-Wide Installation (Optional)

```bash
# Unix/Linux/macOS
sudo cp sshman /usr/local/bin/
sudo chmod +x /usr/local/bin/sshman

# Verify installation
sshman --version
```

Alternatively, add the project directory to your `PATH`:

```bash
# Add to ~/.bashrc or ~/.zshrc
export PATH="$PATH:/path/to/sshman"
```

## 🚀 Quick Start

```bash
# Generate a new ED25519 key (recommended)
sshman generate --name work-github --comment "Work GitHub account"

# List all your SSH keys
sshman list

# Show detailed information about a key
sshman info work-github

# List keys with detailed information
sshman list --long
```

## 📖 Usage

### General Help

```bash
# Show available commands
sshman

# Show detailed help
sshman --help

# Show version
sshman --version
```

### Generate SSH Keys

Generate new SSH keys with various algorithms and options.

```bash
# Generate with default algorithm (ED25519 - most secure and recommended)
sshman generate --name my-key

# Generate RSA key with custom bits
sshman generate --name rsa-key --algo rsa --bits 4096

# Generate ECDSA key
sshman generate --name ecdsa-key --algo ecdsa --bits 384

# Generate with custom comment
sshman generate --name github-key --comment "GitHub account (user@example.com)"

# Generate with passphrase (interactive prompt)
sshman generate --name secure-key --passphrase

# Generate without passphrase (explicitly)
sshman generate --name ci-key --no-passphrase

# Overwrite existing key
sshman generate --name existing-key --force
```

**Supported Algorithms:**
- `ed25519` - Modern, secure, fast (default, recommended)
- `rsa` - Traditional, widely supported (2048-16384 bits, default: 4096)
- `ecdsa` - Elliptic curve (256, 384, or 521 bits, default: 256)

### List SSH Keys

View all SSH keys in your `~/.ssh` directory.

```bash
# Simple list
sshman list

# Detailed list with metadata
sshman list --long

# Include public key files
sshman list --all

# List keys from custom directory
sshman list --path /custom/path/.ssh
```

**Example Output (--long):**

```
SSH Keys in /home/user/.ssh:

  NAME                 TYPE       PERMS      MODIFIED         PUBLIC
  ----------------------------------------------------------------------
  id_ed25519          ED25519    rw-------  2024-01-15 10:30 yes
  id_rsa              RSA        rw-------  2023-12-01 14:22 yes
  work-github         ED25519    rw-------  2024-01-20 09:15 yes

Total: 3 key(s)
```

### Show Key Information

Display comprehensive information about a specific SSH key.

```bash
# Show key information
sshman info my-key

# Show with public key content
sshman info my-key --public

# Show with both SHA256 and MD5 fingerprints
sshman info my-key --fingerprint

# Inspect key in custom directory
sshman info my-key --path /custom/path/.ssh
```

**Example Output:**

```
═══════════════════════════════════════════════════════════════
  SSH Key Information
═══════════════════════════════════════════════════════════════

Name:         work-github
Private Key:  /home/user/.ssh/work-github
Type:         OpenSSH (ED25519/ECDSA/RSA)
Permissions:  rw------- ✓
Size:         2.5 KB
Modified:     2024-01-20 09:15:30
Encrypted:    no

Public Key:   /home/user/.ssh/work-github.pub
Algorithm:    ED25519 (recommended)
Comment:      user@hostname (work-github)
Key Bits:     ~256

Fingerprints:
  SHA256:     SHA256:abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx

═══════════════════════════════════════════════════════════════
```

## 🎯 Common Use Cases

### Setting Up a New GitHub Key

```bash
# Generate a new key for GitHub
sshman generate --name github --comment "GitHub - personal"

# View the public key to copy to GitHub
sshman info github --public

# Verify the key was created successfully
sshman list
```

### Auditing Your SSH Keys

```bash
# List all keys with detailed information
sshman list --long

# Check each key for security issues
sshman info id_rsa
sshman info id_ed25519

# Look for warnings about:
# - Incorrect permissions (should be rw-------)
# - Unencrypted keys (no passphrase)
# - Old or deprecated key types (DSA)
```

### Managing Multiple Keys

```bash
# Generate different keys for different purposes
sshman generate --name work-gitlab --comment "Work GitLab"
sshman generate --name personal-github --comment "Personal GitHub"
sshman generate --name server-deploy --comment "Production server"

# List and organize
sshman list --long
```

## 🏗️ Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn clean test jacoco:report

# Run a specific test
mvn test -Dtest=GenerateCommandTest
```

### Development Mode

```bash
# Build and run with wrapper script
./build.sh
./sshman --help

# Or run directly with Maven
mvn compile exec:java -Dexec.mainClass="com.sshman.SshMan" -Dexec.args="list"

# Run without building JAR (faster for development)
mvn compile exec:java -Dexec.mainClass="com.sshman.SshMan" -Dexec.args="--help"
```

### Project Structure

```
sshman/
├── src/
│   ├── main/java/com/sshman/
│   │   ├── SshMan.java           # Main entry point
│   │   ├── GenerateCommand.java  # Key generation
│   │   ├── ListCommand.java      # List keys
│   │   └── InfoCommand.java      # Key information
│   └── test/java/com/sshman/     # Unit tests
├── pom.xml                        # Maven configuration
├── sshman                         # Wrapper script
└── README.md                      # This file
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Guidelines

1. Ensure tests pass: `mvn test`
2. Follow existing code style
3. Add tests for new features
4. Update documentation as needed

## 📋 Roadmap

See [seeds.md](seeds.md) for the full feature specification and planned enhancements:

- [ ] Profile management for SSH connections
- [ ] SSH config file generation and sync
- [ ] Key rotation and expiry tracking
- [ ] ssh-agent integration
- [ ] Backup and restore functionality
- [ ] JSON output for automation
- [ ] Shell completion (bash, zsh, fish)

## 🐛 Troubleshooting

### "ssh-keygen not found"

Make sure `ssh-keygen` is installed and in your PATH:

```bash
# Check if ssh-keygen is available
which ssh-keygen

# On Debian/Ubuntu
sudo apt-get install openssh-client

# On macOS (usually pre-installed)
ssh-keygen -V
```

### "Key not found"

Make sure you're specifying the correct key name (without the `.pub` extension):

```bash
# Wrong
sshman info id_rsa.pub

# Correct
sshman info id_rsa
```

### Permission Warnings

If you see permission warnings, fix them with:

```bash
chmod 600 ~/.ssh/your-key-name
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [picocli](https://picocli.info/) - A mighty tiny command line interface
- Inspired by the need for better SSH key management workflows

---

**Made with ❤️ for developers who love clean CLIs**
