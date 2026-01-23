# sshman

> A modern, intuitive CLI tool for managing SSH keys and connections

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**sshman** simplifies SSH key management by providing a centralized, user-friendly command-line interface. Generate, list, and inspect SSH keys with ease, featuring security checks, detailed metadata, and beautiful output formatting.

## SSH Commands Wrapped

This tool provides a convenient interface for the following SSH-related commands:

- **`ssh-keygen`** - Generate and inspect SSH keys
- **`ssh-agent`** - Start the SSH authentication agent
- **`ssh-add`** - Manage private keys in the SSH agent
- **`ssh`** - Connect to remote servers using saved profiles (via connect command)
- **`eval`** - Source agent environment variables into the current shell

## Features

- **Generate SSH Keys** - Create ED25519, RSA, or ECDSA keys with sensible defaults
- **Folder Organization** - Organize keys with nested folder structures (e.g., `work/project-a`)
- **Use SSH Keys** - Start ssh-agent and add keys with a single command
- **Auto-Directory Switching** - Automatically load SSH keys per project directory (like `.nvmrc`)
- **Connection Profiles** - Save and manage SSH connection profiles with custom settings
- **List Keys** - View all SSH keys with detailed information (type, permissions, modification dates)
- **Inspect Keys** - Display comprehensive key information including fingerprints and security warnings
- **Security Checks** - Detect insecure file permissions and weak key types
- **Beautiful Output** - Color-coded, well-formatted terminal output
- **Fast & Lightweight** - Single JAR executable with no runtime dependencies

## Installation

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
# target/sshman-0.2.0-jar-with-dependencies.jar

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

### Native Build (No Java Required)

For a standalone executable without Java runtime dependency:

```bash
# Install GraalVM and build native image
./build-native.sh

# The native executable will be at:
# target/sshman

# Install system-wide
sudo cp target/sshman /usr/local/bin/
sudo chmod +x /usr/local/bin/sshman
```

See [NATIVE_BUILD.md](NATIVE_BUILD.md) for detailed instructions on building native executables.

**Benefits:**
- No Java installation required
- Faster startup (~10-50ms vs ~500-1000ms)
- Lower memory usage (~10-30 MB vs ~100-200 MB)
- Single executable binary

### Shell Tab Completion and Auto-Switching (Recommended)

Enable tab completion and auto-directory SSH key switching:

```bash
# Install completion and hooks for your shell
cd completions
./install.sh

# Restart your shell or reload configuration
source ~/.bashrc  # for bash
source ~/.zshrc   # for zsh
```

This installs:
- **Tab completion** for commands and SSH key names
- **Auto-directory switching hooks** that load keys from `.sshman` files
- **Homebrew zsh completion support** for macOS users (auto-detected)

Now you can use:
```bash
./sshman use [TAB]     # Lists available SSH keys
./sshman [TAB]         # Lists available commands
./sshman init [TAB]    # Lists available SSH keys for .sshman file
```

See [completions/README.md](completions/README.md) for detailed installation instructions.

## Quick Start

```bash
# Generate a new ED25519 key with folder organization (recommended)
sshman generate --use work/github --comment "Work GitHub account"

# Start ssh-agent and add the key (recommended workflow)
eval "$(sshman use work/github/id_github_ed25519 --quiet)"

# Enable auto-loading for a project directory
cd ~/projects/work-project
sshman init work/github/id_github_ed25519
# Now this key will auto-load whenever you cd into this directory!

# Generate a simple key with a custom name
sshman generate --name my-key --algo ed25519

# List all your SSH keys (scans all subdirectories)
sshman list

# Show detailed information about a key
sshman info work/github/id_github_ed25519

# List keys with detailed information
sshman list --long

# Create a connection profile interactively
sshman connect-new

# Connect to a saved profile
sshman connect myserver
```

## Usage

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
- If agent is running, **clears all existing keys** and adds the new key (prevents conflicts)
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

**Note on Key Switching:**
When using `sshman use` with an existing ssh-agent, all currently loaded keys are automatically cleared before adding the new key. This ensures only the intended key is active and prevents authentication conflicts when working across different projects.

**Verify the key was added:**

```bash
# List keys currently in ssh-agent
ssh-add -l
```

### Manage SSH Connections

Create and manage SSH connection profiles for easy access to remote servers.

```bash
# Create a new connection profile interactively
sshman connect-new

# You'll be prompted for:
# - Alias (e.g., "myserver", "prod-db")
# - Hostname (e.g., "192.168.1.100", "server.example.com")
# - Username (e.g., "ubuntu", "deploy")
# - Port (default: 22)
# - SSH key path (optional, with auto-suggestions from ~/.ssh)

# Connect to a saved profile
sshman connect myserver

# List available profiles when alias not found
sshman connect nonexistent
# Shows all available profiles and suggests running connect-new
```

**Example Session:**

```
$ sshman connect-new
Create a new SSH connection profile

Alias: production-server
Hostname: prod.example.com
Username: deploy
Port (default: 22): 22
Available SSH keys in /home/user/.ssh:
  - id_ed25519
  - work/id_work_ed25519
  - personal/id_personal_ed25519

SSH key path (optional): work/id_work_ed25519

Profile created successfully!
  Alias:    production-server
  Hostname: prod.example.com
  Username: deploy
  Port:     22
  SSH Key:  /home/user/.ssh/work/id_work_ed25519

To connect, run:
  sshman connect production-server
```

**Connecting to a Profile:**

```bash
$ sshman connect production-server
Connecting to production-server...
Command: ssh -p 22 -i /home/user/.ssh/work/id_work_ed25519 deploy@prod.example.com

# You're now connected to the server
```

**Profile Storage:**
- Profiles are stored in `~/.sshman/profiles.json`
- Tab completion is available for profile aliases
- Profiles include: alias, hostname, username, port, and optional SSH key path

### List Connection Profiles

View all saved SSH connection profiles.

```bash
# List all saved profiles
sshman list-profiles
```

**Example Output:**

```
Available profiles:
  production-server  deploy@prod.example.com:22
  staging-server     ubuntu@staging.example.com:22
  dev-machine        dev@192.168.1.100:22
```

### Archive SSH Keys

Archive unused SSH keys to declutter your `~/.ssh` directory.

```bash
# Archive a key (moves to ~/.ssh/archived/)
sshman archive old-key

# Archive preserves directory structure
sshman archive work/old-project/id_ed25519
```

The archived keys are moved to `~/.ssh/archived/` with their original directory structure preserved.

### Unarchive SSH Keys

Restore previously archived SSH keys.

```bash
# Restore an archived key
sshman unarchive old-key

# Restore with original path
sshman unarchive work/old-project/id_ed25519
```

### Auto-Directory SSH Key Switching

sshman can automatically load SSH keys when you enter directories, similar to how `.sdkmanrc` or `.nvmrc` work. This eliminates the need to manually switch keys when working on different projects.

#### Setup

First, make sure you've installed the shell hooks during completion installation:

```bash
cd completions
./install.sh

# Restart your shell or reload configuration
source ~/.bashrc  # for bash
source ~/.zshrc   # for zsh
```

#### Create .sshman Files

Use the `init` command to create a `.sshman` file in your project directory:

```bash
# Navigate to your project directory
cd ~/projects/work-project

# Create .sshman file with the desired SSH key
sshman init work/gitlab/id_gitlab_ed25519

# The .sshman file is created with the key name
cat .sshman
# Output: work/gitlab/id_gitlab_ed25519
```

#### How It Works

Once you have `.sshman` files in your project directories, the shell hook will automatically:

1. Detect when you `cd` into a directory
2. Search for a `.sshman` file in the current directory or parent directories
3. Read the key name from the file
4. Automatically run `eval "$(sshman use <key-name> --quiet)"`

**Example Workflow:**

```bash
# Create .sshman files for different projects
cd ~/projects/work-project
sshman init work/gitlab/id_gitlab_ed25519

cd ~/projects/personal-project
sshman init personal/github/id_github_ed25519

cd ~/projects/client-deployment
sshman init work/production/deploy/id_deploy_ed25519

# Now, keys are automatically loaded when you enter directories
cd ~/projects/work-project
# Automatically loads: work/gitlab/id_gitlab_ed25519

cd ~/projects/personal-project
# Automatically loads: personal/github/id_github_ed25519

# Verify the current key
ssh-add -l
```

#### Parent Directory Traversal

The hook searches parent directories for `.sshman` files, similar to how git searches for `.git`:

```
~/projects/
├── monorepo/
│   ├── .sshman              # Contains: work/gitlab/id_gitlab_ed25519
│   ├── frontend/
│   │   └── src/
│   └── backend/
│       └── api/

# Entering any subdirectory will use the key from the parent .sshman
cd ~/projects/monorepo/frontend/src  # Uses work/gitlab/id_gitlab_ed25519
cd ~/projects/monorepo/backend/api   # Uses work/gitlab/id_gitlab_ed25519
```

#### Updating .sshman Files

To change the SSH key for a directory, use the `--force` flag:

```bash
cd ~/projects/work-project
sshman init work/new-key --force
```

**Important:** `.sshman` files should be added to your `.gitignore` as they contain user-specific SSH key preferences. sshman automatically adds `.sshman` to the project's `.gitignore` when you run `init` for the first time.

#### Manual Override

You can always manually override the auto-loaded key:

```bash
cd ~/projects/work-project
# Auto-loads: work/gitlab/id_gitlab_ed25519

# Manually switch to a different key
eval "$(sshman use personal/github/id_github_ed25519 --quiet)"
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

### Rotate SSH Keys

Rotate SSH keys by archiving the old key, generating a new one, and automatically updating all references in SSH config and connection profiles.

```bash
# Rotate a single key (preserves key type and comment)
sshman rotate my-key

# Rotate with a new key type
sshman rotate old-rsa-key --type ed25519

# Rotate with custom comment
sshman rotate my-key --comment "New rotated key"

# Rotate multiple keys at once
sshman rotate key1 key2 key3

# Preview changes without executing (dry run)
sshman rotate my-key --dry-run

# Skip confirmation prompts
sshman rotate my-key --force

# Upload new public key to remote servers
sshman rotate my-key --upload user@server1.com,user@server2.com

# Skip connection testing
sshman rotate my-key --no-test

# Skip SSH config backup
sshman rotate my-key --no-backup
```

**What the rotate command does:**

1. **Detects** the original key type and comment from the existing key
2. **Backs up** the SSH config file to `~/.ssh/archived/config_backups/` with timestamp
3. **Tests** the connection with the old key (unless `--no-test` is used)
4. **Generates** a new SSH key with the same or specified type
5. **Tests** the connection with the new key before proceeding
6. **Archives** the old key to `~/.ssh/archived/` (preserving directory structure)
7. **Installs** the new key at the original location
8. **Updates** all SSH config host entries that used the old key
9. **Updates** all connection profiles in `~/.sshman/profiles.json` that used the old key
10. **Uploads** the new public key to specified remote servers (if `--upload` is used)
11. **Logs** all operations to `~/.ssh/rotation.log` in markdown format

**Options:**

- `--type, -t <type>` - Change key type during rotation (ed25519, rsa, ecdsa). Default: preserve original
- `--comment, -c <comment>` - Set comment for new key. Default: preserve original comment
- `--dry-run` - Preview all changes without executing them
- `--force, -f` - Skip all confirmation prompts
- `--upload, -u <targets>` - Upload new public key using ssh-copy-id (comma-separated list)
- `--no-test` - Skip connection testing before archiving old key
- `--no-backup` - Skip backing up SSH config file
- `--path <path>` - Custom SSH directory path

**Example workflow:**

```bash
# Check what will happen
sshman rotate work-key --dry-run

# Perform the rotation
sshman rotate work-key

# Output:
# ========================================
# Rotating key: work-key
# ========================================
#
# Original key type: rsa
# New key type: rsa
# Comment: user@hostname (work-key)
#
# Hosts using this key in SSH config:
#   - production-server
#   - staging-server
#
# Connection profiles using this key:
#   - prod-deploy
#
# ✓ Backed up SSH config to: archived/config_backups/config_20250131_143022
# ✓ Generated new rsa key
# ✓ Archived old key
# ✓ New key installed at: /home/user/.ssh/work-key
# ✓ Updated 2 host(s) in SSH config
# ✓ Updated 1 connection profile(s)
#
# ✓ Key rotation completed successfully: work-key
#
# ========================================
# Rotation Summary
# ========================================
# Total keys processed: 1
# Successful: 1
# Failed: 0
#
# ✓ Rotation log updated: rotation.log
```

**Rotation log file** (`~/.ssh/rotation.log`):

The rotation command maintains a detailed log file in markdown format:

```markdown
# SSH Key Rotation Log

This file contains a history of all SSH key rotations performed by sshman.

## Rotation Session - 2025-01-31 14:30:22

- **[2025-01-31 14:30:22]** BACKUP - SSH config backed up: config_20250131_143022
- **[2025-01-31 14:30:23]** GENERATED - New rsa key for: work-key
- **[2025-01-31 14:30:23]** ARCHIVED - Old key: work-key
- **[2025-01-31 14:30:23]** UPDATED-CONFIG - Updated 2 host(s) for: work-key
- **[2025-01-31 14:30:23]** UPDATED-PROFILES - Updated 1 profile(s) for: work-key
- **[2025-01-31 14:30:23]** SUCCESS - Completed rotation: work-key
```

## Common Use Cases

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

## Development

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
│   │   ├── SshMan.java              # Main entry point
│   │   ├── Profile.java             # Profile data model
│   │   ├── ProfileStorage.java      # Profile persistence
│   │   └── commands/                # Command implementations
│   │       ├── GenerateCommand.java
│   │       ├── ListCommand.java
│   │       ├── InfoCommand.java
│   │       ├── ConnectCommand.java
│   │       └── ...
│   └── test/java/com/sshman/        # Unit tests
├── pom.xml                           # Maven configuration
├── sshman                            # Wrapper script
└── README.md                         # This file
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Development Guidelines

1. Ensure tests pass: `mvn test`
2. Follow existing code style
3. Add tests for new features
4. Update documentation as needed

## Roadmap

See [seeds.md](seeds.md) for the full feature specification and planned enhancements:

- [x] ssh-agent integration (use/set commands)
- [x] Profile management for SSH connections
- [ ] SSH config file generation and sync
- [x] Key rotation and expiry tracking
- [ ] Backup and restore functionality
- [ ] JSON output for automation
- [x] Shell completion (bash, zsh)
- [ ] Shell completion (fish)

## Troubleshooting

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

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Built with [picocli](https://picocli.info/) - A mighty tiny command line interface
- Inspired by the need for better SSH key management workflows

---

**Made for developers who love clean CLIs**
