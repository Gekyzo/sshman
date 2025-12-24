# sshman

> A modern, intuitive CLI tool for managing SSH keys and connections

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**sshman** simplifies SSH key management by providing a centralized, user-friendly command-line interface. Generate, list, and inspect SSH keys with ease, featuring security checks, detailed metadata, and beautiful output formatting.

## ✨ Features

- 🔑 **Generate SSH Keys** - Create ED25519, RSA, or ECDSA keys with sensible defaults
- 📁 **Folder Organization** - Organize keys with nested folder structures (e.g., `work/project-a`)
- 🚀 **Use SSH Keys** - Start ssh-agent and add keys with a single command
- 📋 **List Keys** - View all SSH keys with detailed information (type, permissions, modification dates)
- 🔍 **Inspect Keys** - Display comprehensive key information including fingerprints and security warnings
- 🛡️ **Security Checks** - Detect insecure file permissions and weak key types
- 🎨 **Beautiful Output** - Color-coded, well-formatted terminal output
- ⚡ **Fast & Lightweight** - Single JAR executable with no runtime dependencies

## 📦 Installation

### Prerequisites

- **Java 21 or higher** - [Download Java](https://adoptium.net/)
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
# Generate a new ED25519 key with folder organization (recommended)
sshman generate --use work/github --comment "Work GitHub account"

# Start ssh-agent and add the key (recommended workflow)
eval "$(sshman use work/github/id_github_ed25519 --quiet)"

# Generate a simple key with a custom name
sshman generate --name my-key --algo ed25519

# List all your SSH keys (scans all subdirectories)
sshman list

# Show detailed information about a key
sshman info work/github/id_github_ed25519

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
# Generate with folder organization (RECOMMENDED)
sshman generate --use work/github --algo ed25519

# Generate with nested folders for better organization
sshman generate --use work/client-a/production --algo ed25519

# Generate with custom name in a folder
sshman generate --use personal --name github_key --algo ed25519

# Generate with default algorithm (ED25519 - most secure)
sshman generate --name my-key

# Generate RSA key with custom bits
sshman generate --name rsa-key --algo rsa --bits 4096

# Generate ECDSA key
sshman generate --name ecdsa-key --algo ecdsa --bits 384

# Generate with custom comment
sshman generate --use github --comment "GitHub account (user@example.com)"

# Generate with passphrase (interactive prompt)
sshman generate --use work --passphrase

# Generate without passphrase (explicitly)
sshman generate --use ci/deploy --no-passphrase

# Overwrite existing key
sshman generate --use work --force
```

#### Folder Structure Organization

The `--use` option creates an organized folder structure in `~/.ssh`:

```bash
# Single level organization
sshman generate --use personal --algo ed25519
sshman generate --use work --algo ed25519
sshman generate --use github --algo ed25519

# Creates:
# ~/.ssh/
# ├── personal/
# │   ├── id_personal_ed25519
# │   └── id_personal_ed25519.pub
# ├── work/
# │   ├── id_work_ed25519
# │   └── id_work_ed25519.pub
# └── github/
#     ├── id_github_ed25519
#     └── id_github_ed25519.pub
```

```bash
# Nested folder structure for complex projects
sshman generate --use work/client-a --algo ed25519
sshman generate --use work/client-b --algo ed25519
sshman generate --use personal/hobby-projects --algo ed25519

# Creates:
# ~/.ssh/
# ├── personal/
# │   └── hobby-projects/
# │       ├── id_hobby-projects_ed25519
# │       └── id_hobby-projects_ed25519.pub
# └── work/
#     ├── client-a/
#     │   ├── id_client-a_ed25519
#     │   └── id_client-a_ed25519.pub
#     └── client-b/
#         ├── id_client-b_ed25519
#         └── id_client-b_ed25519.pub
```

**Supported Algorithms:**
- `ed25519` - Modern, secure, fast (default, recommended)
- `rsa` - Traditional, widely supported (2048-16384 bits, default: 4096)
- `ecdsa` - Elliptic curve (256, 384, or 521 bits, default: 256)

### Use SSH Keys (Agent Management)

Start ssh-agent and add SSH keys with auto-completion support.

```bash
# Start ssh-agent and add a key (recommended - use with eval)
eval "$(sshman use CiroPersonal --quiet)"

# Alternative alias (same functionality)
eval "$(sshman set CiroPersonal --quiet)"

# Show what would be executed (helpful output mode)
sshman use CiroPersonal

# Add key with lifetime (expires after 1 hour)
eval "$(sshman use work/github/id_github_ed25519 --quiet --time 3600)"

# Add key with 8 hour lifetime
eval "$(sshman use personal/github/id_github_ed25519 --quiet --time 28800)"

# Use key from custom SSH directory
sshman use my-key --path /custom/path/.ssh --quiet

# Tab completion works for key names (when shell completion is enabled)
sshman use <TAB>  # Shows available SSH keys
```

**How it works:**
- Detects if ssh-agent is already running via `SSH_AUTH_SOCK` and `SSH_AGENT_PID`
- If agent is running, adds the key to the existing agent
- If no agent is running, starts a new agent and adds the key
- `--quiet` mode outputs only the command for use with `eval`
- `--time` sets how long the key remains in the agent (in seconds)

**Example Output (without --quiet):**

```
# Starting ssh-agent and adding key: /home/user/.ssh/CiroPersonal
# Copy and paste the following command, or run:
# eval "$(sshman use CiroPersonal --quiet)"

eval "$(ssh-agent -s)" && ssh-add /home/user/.ssh/CiroPersonal
```

**Verify the key was added:**

```bash
# List keys currently in ssh-agent
ssh-add -l
```

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
# Generate a new key for GitHub with folder organization
sshman generate --use personal/github --comment "GitHub - personal"

# Start ssh-agent and add the key
eval "$(sshman use personal/github/id_github_ed25519 --quiet)"

# View the public key to copy to GitHub
sshman info personal/github/id_github_ed25519 --public

# Verify the key was created successfully
sshman list

# Test the connection
ssh -T git@github.com
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

### Managing Multiple Keys with Folder Structure

```bash
# Generate different keys for different purposes using folder organization
sshman generate --use work/gitlab --comment "Work GitLab"
sshman generate --use personal/github --comment "Personal GitHub"
sshman generate --use work/production/deploy --comment "Production server"

# List all keys (automatically scans subdirectories)
sshman list --long

# The output shows the organized structure:
# work/gitlab/id_gitlab_ed25519
# work/production/deploy/id_deploy_ed25519
# personal/github/id_github_ed25519
```

### Quick Key Switching Between Projects

```bash
# Switch to work GitLab key
eval "$(sshman use work/gitlab/id_gitlab_ed25519 --quiet)"

# Switch to personal GitHub key
eval "$(sshman set personal/github/id_github_ed25519 --quiet)"

# Add production deploy key with 4-hour expiry
eval "$(sshman use work/production/deploy/id_deploy_ed25519 --quiet --time 14400)"

# Verify which keys are currently loaded
ssh-add -l

# Remove all keys from agent when done
ssh-add -D
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

- [x] ssh-agent integration (use/set commands)
- [ ] Profile management for SSH connections
- [ ] SSH config file generation and sync
- [ ] Key rotation and expiry tracking
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
