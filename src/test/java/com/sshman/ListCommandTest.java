package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListCommandTest {
    @Test
    void testListCommand() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("list");
        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandLongFormat() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("list", "-l");
        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandInvalidPath() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setErr(new PrintWriter(sw));

        int exitCode = cmd.execute("list", "--path", "/nonexistent/path");

        assertEquals(1, exitCode);
    }
}
