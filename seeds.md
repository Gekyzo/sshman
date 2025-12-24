# SSH Centralized CLI – Features & Specifications

## Overview

A terminal-based utility to centralize and simplify SSH-related workflows such as key management, host profiles, secure connections, and automation. The tool acts as a single entry point for generating, managing, and using SSH keys and connections in a consistent, secure, and scriptable way.

---

## Goals

- Centralize SSH key and connection management
- Reduce manual SSH configuration and errors
- Improve security visibility and hygiene
- Provide a fast, scriptable, Unix-friendly CLI
- Remain extensible for future use cases

---

## Core Concepts

### Key

A managed SSH key with metadata (type, fingerprint, last-used, expiry, alias).

### Profile

A named SSH connection definition (host, user, port, key, options).

### Session

A single SSH execution using a resolved profile and key.

---

## Features

### 1. SSH Key Management

- Generate SSH keys

  - Supported types: RSA, ED25519, ECDSA
  - Configurable size/strength
  - Optional passphrase support

- List existing keys
- Rename keys using aliases
- Delete or archive keys
- Import existing keys
- Export public keys
- Display fingerprints and metadata
- Mark default key

---

### 2. Key Usage & Agent Integration

- Assign keys to specific hosts or profiles
- Temporary key usage for one-off sessions
- Integration with `ssh-agent`

  - Add/remove keys automatically
  - Detect running agent

- Enforce key usage (fail if incorrect key is used)

---

### 3. Host & Profile Management

- Create and manage SSH profiles
- Profile attributes:

  - Hostname / IP
  - Username
  - Port
  - SSH key
  - Extra SSH options

- Profile aliases for fast access
- Group profiles (e.g. prod, staging, personal)
- Environment-based profiles
- Profile templates

---

### 4. Connection & Command Abstractions

- One-command SSH connection via profile
- Dry-run mode (print resolved SSH command)
- Run predefined remote commands
- SCP / SFTP using profiles
- Port forwarding presets (local / remote)
- Parallel execution across multiple hosts

---

### 5. SSH Configuration Management

- Generate and maintain `~/.ssh/config`
- Sync tool configuration with SSH config
- Validate SSH config syntax
- Override or extend global SSH options
- Support include-based SSH configs

---

### 6. Security & Compliance

- Detect weak or deprecated key types
- Warn about insecure file permissions
- Key rotation support
- Optional expiry metadata for keys
- Connection and key usage audit log

---

### 7. CLI & User Experience

- Simple, discoverable command structure
- Interactive selection (fzf-style)
- Shell auto-completion (bash, zsh, fish)
- Verbose and debug modes
- Colorized output and warnings
- Machine-readable output (JSON)

---

### 8. Automation & CI Support

- Non-interactive execution mode
- Exit codes suitable for scripting
- JSON output for automation tools
- CI-safe key handling

---

### 9. Backup & Sync

- Backup configuration and metadata
- Optional encrypted backups
- Export sanitized configs (no private keys)
- Cross-machine profile portability

---

### 10. Extensibility

- Plugin system for custom commands
- Hook support:

  - Pre-connect
  - Post-connect

- Config file support (YAML / TOML / JSON)
- Environment variable overrides

---

### 11. Observability

- Connection history
- Last-used timestamps per key
- Usage statistics per host and key

---

## Non-Goals (Initial Versions)

- SSH server management
- GUI interface
- Cloud-provider-specific abstractions

---

## Target Platforms

- Linux
- macOS
- Windows (via WSL)

---

## Design Principles

- Explicit over implicit
- Secure by default
- Minimal dependencies
- Unix philosophy compliant
- Fast startup time

---

## Future Enhancements

- Encrypted key vault
- Hardware key support (YubiKey)
- Secrets manager integration
- Team/shared profile support
