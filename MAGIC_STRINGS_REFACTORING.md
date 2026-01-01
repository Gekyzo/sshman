# Magic Strings Refactoring Summary

## Overview
This document summarizes the extraction of magic strings from command classes into a centralized constants file (`SshManConstants.java`).

## Constants Created

### File Extensions
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `".pub"` | `FileExtensions.PUBLIC_KEY` | Public key files |
| `".meta"` | `FileExtensions.METADATA` | Metadata files |
| `".db"` | `FileExtensions.DATABASE` | Database files |
| `".old"` | `FileExtensions.OLD` | Old/backup files |
| `".bak"` | `FileExtensions.BACKUP` | Backup files |

### Directory Names
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `".ssh"` | `DirectoryNames.SSH` | SSH configuration directory |
| `"archived"` | `DirectoryNames.ARCHIVED` | Archived keys directory |
| `"config_backups"` | `DirectoryNames.CONFIG_BACKUPS` | Config backups directory |

### File Names
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"config"` | `FileNames.CONFIG` | SSH configuration file |
| `"known_hosts"` | `FileNames.KNOWN_HOSTS` | Known hosts file |
| `"known_hosts.old"` | `FileNames.KNOWN_HOSTS_OLD` | Old known hosts file |
| `"authorized_keys"` | `FileNames.AUTHORIZED_KEYS` | Authorized keys file |
| `".sshman"` | `FileNames.SSHMAN_FILE` | Project SSH config file |
| `"rotation.log"` | `FileNames.ROTATION_LOG` | Key rotation log file |

### Private Key Headers
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"-----BEGIN"` | `PrivateKeyHeaders.BEGIN_PREFIX` | PEM header start |
| `"-----BEGIN".getBytes()` | `PrivateKeyHeaders.PRIVATE_KEY_MAGIC` | Magic bytes for detection |
| `"OPENSSH PRIVATE KEY"` | `PrivateKeyHeaders.OPENSSH_PRIVATE_KEY` | OpenSSH format |
| `"RSA PRIVATE KEY"` | `PrivateKeyHeaders.RSA_PRIVATE_KEY` | RSA format |
| `"EC PRIVATE KEY"` | `PrivateKeyHeaders.EC_PRIVATE_KEY` | EC format |
| `"DSA PRIVATE KEY"` | `PrivateKeyHeaders.DSA_PRIVATE_KEY` | DSA format |

### SSH Config Keywords
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"host "` | `SshConfigKeywords.HOST` | Host directive |
| `"identityfile "` | `SshConfigKeywords.IDENTITY_FILE` | IdentityFile directive |

### Environment Variables
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"SSH_AUTH_SOCK"` | `EnvironmentVariables.SSH_AUTH_SOCK` | SSH agent socket |
| `"SSH_AGENT_PID"` | `EnvironmentVariables.SSH_AGENT_PID` | SSH agent PID |
| `"SSH_ASKPASS"` | `EnvironmentVariables.SSH_ASKPASS` | SSH password prompt |
| `"SSH_ASKPASS_REQUIRE"` | `EnvironmentVariables.SSH_ASKPASS_REQUIRE` | Password prompt requirement |

### System Properties
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"user.home"` | `SystemProperties.USER_HOME` | User home directory |
| `"user.name"` | `SystemProperties.USER_NAME` | User name |
| `"user.dir"` | `SystemProperties.USER_DIR` | Current working directory |

### Path Patterns
| Magic String | Constant | Usage |
|--------------|----------|-------|
| `"~/"` | `PathPatterns.HOME_PREFIX` | Home directory prefix |
| `"~/.ssh/"` | `PathPatterns.SSH_DIR_PATTERN` | SSH directory pattern |

## Files Refactored

### Completed
1. ✅ `SshKeyUtils.java`
2. ✅ `ArchiveCommand.java`
3. ✅ `UnarchiveCommand.java`
4. ✅ `InitCommand.java`
5. ✅ `UseCommand.java`
6. ✅ `ConnectNewCommand.java`

### Remaining
- `ListCommand.java` - Has `.pub`, `.meta`, `archived`, `config`, `known_hosts`, etc.
- `InfoCommand.java` - Has `.pub`, key algorithm strings, encryption markers
- `GenerateCommand.java` - Has `.pub`, system properties, key type strings
- `RotateCommand.java` - Has many strings (large file)

## Benefits

1. **Reduced Duplication**: Eliminates repeated string literals across codebase
2. **Type Safety**: Compile-time checking instead of runtime errors
3. **Maintainability**: Single source of truth for constants
4. **Refactoring Safety**: IDE can find all usages
5. **Documentation**: Constants are self-documenting with JavaDoc

## Backward Compatibility

All refactorings maintain 100% backward compatibility:
- No behavioral changes
- Same string values used
- No API changes
- All tests should pass unchanged
