# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- SSH key rotation with `rotate` command
  - Automatic key type and comment detection from existing keys
  - Option to change key type during rotation with `--type` flag
  - Custom comment support with `--comment` flag
  - Batch rotation support for multiple keys at once
  - Dry-run mode to preview changes with `--dry-run` flag
  - Automatic SSH config file backup to `~/.ssh/archived/config_backups/` with timestamps
  - Config backup history preservation
  - Automatic updates to all SSH config host entries using the rotated key
  - Automatic updates to all connection profiles in `~/.sshman/profiles.json`
  - Connection testing before and after rotation (with `--no-test` to skip)
  - Public key upload to remote servers with `--upload` flag using ssh-copy-id
  - Comprehensive markdown logging to `~/.ssh/rotation.log`
  - Tab completion support for key selection
  - Rotation summary showing success/failure counts
  - Preserves directory structure when archiving old keys
  - Proper permissions handling (600 for private keys, 644 for public keys)

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

[0.1.1]: https://github.com/Gekyzo/sshman/releases/tag/v0.1.1
[0.1.0]: https://github.com/Gekyzo/sshman/releases/tag/v0.1.0
[0.0.1]: https://github.com/Gekyzo/sshman/releases/tag/v0.0.1
