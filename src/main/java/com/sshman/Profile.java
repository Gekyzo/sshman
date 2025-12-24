package com.sshman;

public class Profile {
    private String alias;
    private String hostname;
    private String username;
    private Integer port;
    private String sshKey;

    public Profile() {
    }

    public Profile(String alias, String hostname, String username, Integer port, String sshKey) {
        this.alias = alias;
        this.hostname = hostname;
        this.username = username;
        this.port = port;
        this.sshKey = sshKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

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
}
