package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SshManTest {

    @Test
    void testVersionOption() {
        SshMan app = new SshMan();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute("--version");
        assertEquals(0, exitCode);
    }

    @Test
    void testHelpOption() {
        SshMan app = new SshMan();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
    }

    @Test
    void testNoArguments() {
        SshMan app = new SshMan();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute();
        assertEquals(0, exitCode);
    }

    @Test
    void testShortVersionFlag() {
        SshMan app = new SshMan();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute("-V");
        assertEquals(0, exitCode);
    }

    @Test
    void testShortHelpFlag() {
        SshMan app = new SshMan();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute("-h");
        assertEquals(0, exitCode);
    }
}
