package com.sshman;

/**
 * Represents an SSH connection profile.
 */
public record Profile(
    String alias,
    String hostname,
    String username,
    Integer port,
    String sshKey
) {
    /**
     * Generates the equivalent SSH command for this profile.
     */
    public String toSshCommand() {
        StringBuilder cmd = new StringBuilder("ssh");

        if (port != null && port != 22) {
            cmd.append(" -p ").append(port);
        }

        if (sshKey != null && !sshKey.isEmpty()) {
            cmd.append(" -i ").append(sshKey);
        }

        cmd.append(" ").append(username).append("@").append(hostname);

        return cmd.toString();
    }

    /**
     * Creates a new Profile with a different SSH key path.
     * Used for immutable updates during key rotation.
     */
    public Profile withSshKey(String newSshKey) {
        return new Profile(alias, hostname, username, port, newSshKey);
    }
}
