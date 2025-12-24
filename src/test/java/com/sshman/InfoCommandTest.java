package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoCommandTest {

    @Test
    void testInfoHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("info", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("--public"));
    }

    @Test
    void testInfoMissingKeyName() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("info");
        assertEquals(2, exitCode); // Missing required parameter
    }

    @Test
    void testInfoNonexistentKey() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("info", "nonexistent-key-12345");
        assertEquals(1, exitCode);
    }

}
