package com.sshman.constants;

/**
 * Centralized constants for SSH key management.
 * This class consolidates all magic strings used throughout the application
 * to reduce duplication and improve maintainability.
 */
public final class SshManConstants {

    private SshManConstants() {
        // Utility class - prevent instantiation
    }

    // ========================================================================
    // File Extensions
    // ========================================================================

    /**
     * File extensions used in SSH key management.
     */
    public static final class FileExtensions {
        private FileExtensions() {}

        /** Public key file extension */
        public static final String PUBLIC_KEY = ".pub";

        /** Metadata file extension */
        public static final String METADATA = ".meta";

        /** Database file extension */
        public static final String DATABASE = ".db";

        /** Old/backup file extension */
        public static final String OLD = ".old";

        /** Backup file extension */
        public static final String BACKUP = ".bak";
    }

    // ========================================================================
    // Directory Names
    // ========================================================================

    /**
     * Standard directory names used in SSH configuration.
     */
    public static final class DirectoryNames {
        private DirectoryNames() {}

        /** SSH configuration directory name */
        public static final String SSH = ".ssh";

        /** Archived keys directory name */
        public static final String ARCHIVED = "archived";

        /** Config backups directory name */
        public static final String CONFIG_BACKUPS = "config_backups";
    }

    // ========================================================================
    // File Names
    // ========================================================================

    /**
     * Standard file names in SSH directory.
     */
    public static final class FileNames {
        private FileNames() {}

        /** SSH configuration file */
        public static final String CONFIG = "config";

        /** Known hosts file */
        public static final String KNOWN_HOSTS = "known_hosts";

        /** Old known hosts file */
        public static final String KNOWN_HOSTS_OLD = "known_hosts.old";

        /** Authorized keys file */
        public static final String AUTHORIZED_KEYS = "authorized_keys";

        /** Project-level SSH key configuration file */
        public static final String SSHMAN_FILE = ".sshman";

        /** Key rotation log file */
        public static final String ROTATION_LOG = "rotation.log";
    }

    // ========================================================================
    // Private Key Headers
    // ========================================================================

    /**
     * Magic bytes and headers for identifying private SSH keys.
     */
    public static final class PrivateKeyHeaders {
        private PrivateKeyHeaders() {}

        /** Standard PEM header start */
        public static final String BEGIN_PREFIX = "-----BEGIN";

        /** Magic bytes for private key detection */
        public static final byte[] PRIVATE_KEY_MAGIC = BEGIN_PREFIX.getBytes();

        /** OpenSSH format private key header */
        public static final String OPENSSH_PRIVATE_KEY = "OPENSSH PRIVATE KEY";

        /** OpenSSH encrypted private key header */
        public static final String SSH2_ENCRYPTED_PRIVATE_KEY = "SSH2 ENCRYPTED PRIVATE KEY";

        /** RSA private key header */
        public static final String RSA_PRIVATE_KEY = "RSA PRIVATE KEY";

        /** EC private key header */
        public static final String EC_PRIVATE_KEY = "EC PRIVATE KEY";

        /** DSA private key header */
        public static final String DSA_PRIVATE_KEY = "DSA PRIVATE KEY";

        /** Generic PKCS#8 private key header */
        public static final String PRIVATE_KEY = "PRIVATE KEY";

        /** Full OpenSSH header */
        public static final String BEGIN_OPENSSH_PRIVATE_KEY = "BEGIN OPENSSH PRIVATE KEY";

        /** Full SSH2 encrypted header */
        public static final String BEGIN_SSH2_ENCRYPTED_PRIVATE_KEY = "BEGIN SSH2 ENCRYPTED PRIVATE KEY";

        /** Full RSA header */
        public static final String BEGIN_RSA_PRIVATE_KEY = "BEGIN RSA PRIVATE KEY";

        /** Full EC header */
        public static final String BEGIN_EC_PRIVATE_KEY = "BEGIN EC PRIVATE KEY";

        /** Full DSA header */
        public static final String BEGIN_DSA_PRIVATE_KEY = "BEGIN DSA PRIVATE KEY";
    }

    // ========================================================================
    // Encryption Markers
    // ========================================================================

    /**
     * Markers indicating encrypted private keys.
     */
    public static final class EncryptionMarkers {
        private EncryptionMarkers() {}

        /** Generic encryption marker */
        public static final String ENCRYPTED = "ENCRYPTED";

        /** PEM encryption type marker */
        public static final String PROC_TYPE_ENCRYPTED = "Proc-Type: 4,ENCRYPTED";

        /** PEM DEK info marker */
        public static final String DEK_INFO = "DEK-Info:";
    }

    // ========================================================================
    // SSH Config Keywords
    // ========================================================================

    /**
     * Keywords used in SSH configuration files.
     * Note: SSH config is case-insensitive, but we use lowercase for matching.
     */
    public static final class SshConfigKeywords {
        private SshConfigKeywords() {}

        /** Host directive (with trailing space) */
        public static final String HOST = "host ";

        /** IdentityFile directive (with trailing space) */
        public static final String IDENTITY_FILE = "identityfile ";
    }

    // ========================================================================
    // SSH Key Algorithms
    // ========================================================================

    /**
     * SSH key algorithm identifiers as they appear in public keys.
     */
    public static final class KeyAlgorithms {
        private KeyAlgorithms() {}

        /** ED25519 algorithm */
        public static final String SSH_ED25519 = "ssh-ed25519";

        /** RSA algorithm */
        public static final String SSH_RSA = "ssh-rsa";

        /** ECDSA P-256 curve */
        public static final String ECDSA_SHA2_NISTP256 = "ecdsa-sha2-nistp256";

        /** ECDSA P-384 curve */
        public static final String ECDSA_SHA2_NISTP384 = "ecdsa-sha2-nistp384";

        /** ECDSA P-521 curve */
        public static final String ECDSA_SHA2_NISTP521 = "ecdsa-sha2-nistp521";

        /** DSA algorithm (deprecated) */
        public static final String SSH_DSS = "ssh-dss";
    }

    // ========================================================================
    // Key Type Names
    // ========================================================================

    /**
     * Key type names used in key generation and detection.
     */
    public static final class KeyTypes {
        private KeyTypes() {}

        /** ED25519 key type */
        public static final String ED25519 = "ed25519";

        /** RSA key type */
        public static final String RSA = "rsa";

        /** ECDSA key type */
        public static final String ECDSA = "ecdsa";

        /** DSA key type */
        public static final String DSA = "dsa";
    }

    // ========================================================================
    // Environment Variables
    // ========================================================================

    /**
     * Environment variables used by SSH and ssh-agent.
     */
    public static final class EnvironmentVariables {
        private EnvironmentVariables() {}

        /** SSH authentication socket */
        public static final String SSH_AUTH_SOCK = "SSH_AUTH_SOCK";

        /** SSH agent process ID */
        public static final String SSH_AGENT_PID = "SSH_AGENT_PID";

        /** SSH password prompt program */
        public static final String SSH_ASKPASS = "SSH_ASKPASS";

        /** SSH password prompt requirement */
        public static final String SSH_ASKPASS_REQUIRE = "SSH_ASKPASS_REQUIRE";
    }

    // ========================================================================
    // System Properties
    // ========================================================================

    /**
     * Java system properties commonly used.
     */
    public static final class SystemProperties {
        private SystemProperties() {}

        /** User home directory */
        public static final String USER_HOME = "user.home";

        /** User name */
        public static final String USER_NAME = "user.name";

        /** Current working directory */
        public static final String USER_DIR = "user.dir";
    }

    // ========================================================================
    // Path Patterns
    // ========================================================================

    /**
     * Common path patterns used in SSH configuration.
     */
    public static final class PathPatterns {
        private PathPatterns() {}

        /** Home directory prefix in SSH config */
        public static final String HOME_PREFIX = "~/";

        /** Standard SSH directory path pattern */
        public static final String SSH_DIR_PATTERN = "~/.ssh/";
    }

    // ========================================================================
    // Key Name Patterns
    // ========================================================================

    /**
     * Patterns used in SSH key naming.
     */
    public static final class KeyNamePatterns {
        private KeyNamePatterns() {}

        /** Default key name prefix */
        public static final String ID_PREFIX = "id_";
    }
}
