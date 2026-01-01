# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-01-01

### Added
- SSH key rotation with `rotate` command
  - Automatic key type and comment detection from existing keys
  - Option to change key type during rotation with `--type` flag
  - Custom comment support with `--comment` flag
  - Batch rotation support for multiple keys at once
  - Dry-run mode to preview changes with `--dry-run` flag
  - Interactive confirmation prompts for safety (with `--force` to skip)
  - Force mode with `--force` flag to skip all confirmation prompts
  - Automatic SSH config file backup to `~/.ssh/archived/config_backups/` with timestamps
  - Config backup history preservation
  - Option to skip config backup with `--no-backup` flag
  - Automatic updates to all SSH config host entries using the rotated key
  - Automatic updates to all connection profiles in `~/.sshman/profiles.json`
  - Connection testing before and after rotation (with `--no-test` to skip)
  - Public key upload to remote servers with `--upload` flag using ssh-copy-id
  - Support for multiple upload targets (comma-separated)
  - Comprehensive markdown logging to `~/.ssh/rotation.log`
  - Custom SSH directory support with `--path` flag
  - Tab completion support for key selection
  - Rotation summary showing success/failure counts
  - Preserves directory structure when archiving old keys
  - Proper permissions handling (600 for private keys, 644 for public keys)
- SSH key archiving with `archive` command
  - Move inactive SSH keys to `~/.ssh/archived/` directory
  - Preserves directory structure when archiving keys
  - Archives both private and public key files
  - Detects if key is referenced in `~/.ssh/config` and warns affected hosts
  - Interactive confirmation prompt when archiving keys in use
  - Force mode with `--force` flag to skip confirmation
  - Automatic cleanup of empty directories after archiving
  - Recursive key search for keys in subdirectories
  - Custom SSH directory support with `--path` flag
  - Tab completion for active key selection
  - Lists available keys when specified key is not found
- SSH key unarchiving with `unarchive` command
  - Restore archived keys from `~/.ssh/archived/` back to active use
  - Restores both private and public key files
  - Preserves original directory structure when restoring
  - Detects and prevents overwriting existing keys (unless `--force` is used)
  - Force mode with `--force` flag to overwrite existing keys
  - Creates parent directories if needed during restoration
  - Automatic cleanup of empty directories in archive after restoration
  - Recursive search for archived keys in subdirectories
  - Custom SSH directory support with `--path` flag
  - Tab completion for archived key selection
  - Lists available archived keys when specified key is not found
- Native GraalVM build support
  - Build native executables with `./build-native.sh` or `mvn -Pnative package`
  - Native binaries for Linux, macOS, and Windows
  - No Java runtime required for native binaries
  - ~10-50ms startup time vs ~500-1000ms for JAR
  - ~10-30 MB memory usage vs ~100-200 MB for JAR
  - Comprehensive native build documentation in NATIVE_BUILD.md
  - GraalVM native-image configuration for reflection and resources
- Structured logging with SLF4J and Logback
  - Persistent logging to `~/.ssh/sshman.log`
  - Automatic log file creation and management
  - Timestamped log entries for audit trail
  - Log levels (INFO, WARN, ERROR) for different operations

### Improved
- Display equivalent SSH command for all operations
- Enhanced error messages with actionable suggestions
- Better user feedback during key operations

## [0.1.1] - 2025-12-24

### Added
- Connection profile management with `connect-new` and `connect` commands
- Profile storage in `~/.sshman/profiles.json`
- Tab completion support for connection profile aliases
- CHANGELOG.md documenting all project changes
- ROADMAP.md outlining future features and planned improvements
- GitHub Actions workflow for automated releases

### Improved
- Documentation with comprehensive connection profile examples
- README with SSH commands reference section

### Technical Debt
- Migrated feature specifications from seeds.md to CHANGELOG and ROADMAP

## [0.1.0] - 2024-01-20

### Added
- SSH key generation with `generate` command
  - Support for ED25519, RSA, and ECDSA algorithms
  - Folder-based organization using `--use` flag
  - Nested folder structures for complex project hierarchies
  - Custom key naming with `--name` flag
  - Passphrase support with interactive prompts
  - Force overwrite option with `--force` flag
- SSH key listing with `list` command
  - Simple and detailed output modes (`--long`)
  - Automatic subdirectory scanning
  - Metadata display (type, permissions, modification dates)
  - Custom directory support with `--path` flag
- Key inspection with `info` command
  - Comprehensive key metadata display
  - Fingerprint generation (SHA256 and MD5)
  - Security warnings for insecure permissions
  - Public key display option with `--public` flag
- SSH agent management with `use` command
  - Automatic ssh-agent startup
  - Key lifetime support with `--time` flag
  - Quiet mode for use with `eval` command
  - Auto-clear existing keys before adding new ones
  - Alias `set` command for `use`
- Auto-directory SSH key switching
  - `init` command to create `.sshman` files in project directories
  - Shell hooks for automatic key loading on directory change
  - Parent directory traversal for `.sshman` file discovery
  - Automatic `.gitignore` updates
- Shell completion support
  - Tab completion for bash and zsh
  - Command, key name, and profile alias completion
  - Homebrew zsh completion support for macOS users
- GitHub Actions workflow for automated releases
- Comprehensive test suite
- Documentation website (GitHub Pages)

### Changed
- Upgraded Java requirement from 17 to 21

### Security
- Permission validation for SSH keys (must be `rw-------` or `600`)
- Warning for unencrypted private keys
- Security checks for weak or deprecated key types

## [0.0.1] - Initial Development

### Added
- Initial project setup with Maven
- Basic CLI framework using picocli
- Project structure and build configuration

[0.2.0]: https://github.com/Gekyzo/sshman/releases/tag/v0.2.0
[0.1.1]: https://github.com/Gekyzo/sshman/releases/tag/v0.1.1
[0.1.0]: https://github.com/Gekyzo/sshman/releases/tag/v0.1.0
[0.0.1]: https://github.com/Gekyzo/sshman/releases/tag/v0.0.1
