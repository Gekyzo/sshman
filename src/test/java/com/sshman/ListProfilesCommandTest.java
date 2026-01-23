package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListProfilesCommandTest {
    @Test
    void testListProfilesBasic() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("list-profiles");
        assertEquals(0, exitCode);
    }

    @Test
    void testListProfilesLongFormat() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("list-profiles", "-l");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("ALIAS") || output.contains("No SSH connection profiles found"));
    }

    @Test
    void testListProfilesShortFormat() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("list-profiles");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("SSH Connection Profiles") || output.contains("No SSH connection profiles found"));
    }

    @Test
    void testListProfilesHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("list-profiles", "--help");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("List all saved SSH connection profiles"));
    }

    @Test
    void testListProfilesLongFlagAlias() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("list-profiles", "--long");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("ALIAS") || output.contains("No SSH connection profiles found"));
    }
}
