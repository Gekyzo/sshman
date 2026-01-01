# Roadmap

This document outlines the planned features and improvements for sshman.

## Version 0.2.0 - Enhanced Connection Management

**Target:** Q1 2026

### Connection Management
- [ ] Edit existing connection profiles
- [ ] Delete connection profiles
- [ ] List all saved connection profiles with details
- [ ] Export/import connection profiles
- [ ] Connection profile validation and testing
- [ ] Support for SSH connection options (compression, forwarding, etc.)
- [ ] Connection history tracking

### SSH Config Integration
- [ ] Generate SSH config file from saved profiles
- [ ] Sync connection profiles with existing SSH config
- [ ] Import connections from SSH config file
- [ ] Validate SSH config syntax
- [ ] Backup and restore SSH config

## Version 0.3.0 - Advanced Key Management

**Target:** Q2 2026

### Key Rotation & Lifecycle
- [ ] Key expiry tracking and notifications
- [ ] Key usage statistics and audit logs
- [ ] Warn about old or unused keys
- [ ] Key lifecycle management (creation date, last used, etc.)

### Backup & Recovery
- [ ] Backup SSH keys to encrypted archive
- [ ] Restore keys from backup
- [ ] Cloud backup integration (optional)
- [ ] Backup verification and integrity checks

### Key Operations
- [ ] Rename SSH keys
- [ ] Move keys between folders
- [ ] Copy keys to remote servers (ssh-copy-id wrapper)
- [ ] Remove/delete keys safely
- [ ] Batch operations on multiple keys
- [ ] Deploy public key to remote server's authorized_keys

## Version 0.4.0 - Developer Experience

**Target:** Q2 2026

### Shell Completion
- [ ] Fish shell completion support
- [ ] PowerShell completion support
- [ ] Enhanced completion with descriptions
- [ ] Dynamic completion for remote hosts

### Output & Integration
- [ ] JSON output mode for all commands (`--json`)
- [ ] YAML output format support
- [ ] Machine-readable output for automation
- [ ] Quiet mode improvements
- [ ] Verbose/debug output mode

### User Experience
- [ ] Interactive mode for all commands
- [ ] Configuration file support (`~/.sshman/config`)
- [ ] Custom default settings (algorithm, key size, etc.)
- [ ] Color scheme customization
- [ ] Localization support
- [ ] Interactive TUI with fuzzy search for profiles and keys
- [ ] Quick connect with partial name matching

## Version 0.5.0 - Security & Compliance

**Target:** Q3 2026

### Security Features
- [ ] Hardware security key (YubiKey/FIDO2/U2F) integration
- [ ] Support for sk-ssh-ed25519@openssh.com key types
- [ ] SSH certificate support
- [ ] Key signing and verification
- [ ] Security policy enforcement
- [ ] Compliance reporting
- [ ] Time-limited ssh-agent sessions (TTL support)
- [ ] Automatic key removal from memory after timeout

### Auditing
- [ ] Comprehensive audit logging
- [ ] Security scanning for all keys
- [ ] Weak key detection and alerts
- [ ] Key compromise detection
- [ ] Compliance with security standards (NIST, etc.)

## Version 1.0.0 - Production Ready

**Target:** Q4 2026

### Platform Support
- [ ] Windows native support
- [ ] Cross-platform installer
- [ ] Package managers (Homebrew, apt, yum, Chocolatey)
- [ ] Docker image
- [ ] Snap/Flatpak packages

### Documentation
- [ ] Comprehensive user guide
- [ ] Video tutorials
- [ ] API documentation
- [ ] Migration guides from other tools
- [ ] Best practices guide

### Enterprise Features
- [ ] Team collaboration features
- [ ] Centralized key management server
- [ ] LDAP/Active Directory integration
- [ ] Role-based access control
- [ ] Multi-tenancy support

## Future Considerations

### Advanced Features
- [ ] SSH tunnel management
- [ ] Port forwarding wizard (simplified -L/-R interface)
- [ ] Port forwarding profiles
- [ ] Bastion/jump host support (ProxyJump configuration)
- [ ] Multi-hop SSH connections
- [ ] SSH session recording
- [ ] SCP/SFTP wrapper using connection profiles
- [ ] Known hosts cleaner utility (fix offending key errors)
- [ ] Connection doctor (verbose diagnostics and error analysis)
- [ ] Dry-run mode for all commands (show command without executing)

### Integration
- [ ] Git provider integration (GitHub, GitLab, Bitbucket)
- [ ] API integration to upload public keys to Git providers
- [ ] Cloud provider CLI integration (AWS, GCP, Azure)
- [ ] Cloud inventory import (auto-generate profiles from EC2/GCP/DO instances)
- [ ] Container orchestration tools (Kubernetes, Docker)
- [ ] Infrastructure as Code tools (Terraform, Ansible)

### Automation
- [ ] Key provisioning automation
- [ ] Automated key deployment to servers
- [ ] Integration with secret management tools (Vault, etc.)
- [ ] Webhooks and event triggers
- [ ] CI/CD pipeline integration

## Completed Features

### Version 0.2.0 (Unreleased)
- [x] SSH key rotation with automated workflows
- [x] SSH key archiving to `~/.ssh/archived/`
- [x] SSH key unarchiving and restoration
- [x] SSH config backup during rotation
- [x] Connection profile updates during rotation
- [x] Public key deployment to remote servers

### Version 0.1.1
- [x] Connection profile management (connect-new, connect commands)
- [x] Profile storage in `~/.sshman/profiles.json`
- [x] Tab completion for connection profiles

### Version 0.1.0
- [x] SSH key generation (ED25519, RSA, ECDSA)
- [x] Folder-based key organization
- [x] SSH key listing with metadata
- [x] Key inspection and fingerprints
- [x] ssh-agent integration (use/set commands)
- [x] Auto-directory SSH key switching
- [x] Shell completion (bash, zsh)
- [x] Security checks and warnings
- [x] GitHub Actions CI/CD
- [x] Documentation website

## How to Contribute

If you'd like to help implement any of these features:

1. Check the [GitHub Issues](https://github.com/Gekyzo/sshman/issues) for existing work
2. Comment on an issue to indicate your interest
3. Fork the repository and create a feature branch
4. Submit a pull request with your changes

For major features, please open an issue first to discuss the implementation approach.

## Feedback

Have ideas for features not listed here? [Open an issue](https://github.com/Gekyzo/sshman/issues/new) and let us know!
