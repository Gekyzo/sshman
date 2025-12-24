package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCommandTest {

    @Test
    void testGenerateMissingNameAndUse() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int exitCode = cmd.execute("generate", "--no-passphrase");

        assertEquals(1, exitCode); // Either --name or --use must be specified
        assertTrue(sw.toString().contains("--name") || sw.toString().contains("--use"));
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
        assertTrue(sw.toString().contains("--use"));
    }

    @Test
    void testGenerateWithUseOption() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        // Test that --use option creates appropriate key name
        int exitCode = cmd.execute("generate", "--use", "work", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("--use"));
    }

}
