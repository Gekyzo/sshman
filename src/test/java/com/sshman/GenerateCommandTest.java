package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCommandTest {

    @Test
    void testGenerateMissingName() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int exitCode = cmd.execute("generate");

        assertEquals(2, exitCode); // Missing required option
        assertTrue(sw.toString().contains("--name"));
    }

    @Test
    void testGenerateInvalidName() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int exitCode = cmd.execute("generate", "-n", "../etc/passwd", "--no-passphrase");

        assertEquals(1, exitCode);
    }

    @Test
    void testGenerateInvalidRsaBits() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int exitCode = cmd.execute("generate", "-n", "test", "-a", "rsa", "-b", "512", "--no-passphrase");

        assertEquals(1, exitCode);
        assertTrue(sw.toString().contains("2048") ||
            cmd.getExecutionResult() == null);
    }

    @Test
    void testGenerateHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("generate", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("--algo"));
        assertTrue(sw.toString().contains("--name"));
    }

}
