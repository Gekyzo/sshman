package com.sshman;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseCommandTest {

    @Test
    void testUseHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("use", "--help");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("Start ssh-agent"));
        assertTrue(output.contains("--quiet"));
        assertTrue(output.contains("--time"));
    }

    @Test
    void testUseMissingKeyName() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("use");
        assertEquals(2, exitCode); // Missing required parameter
    }

    @Test
    void testUseNonexistentKey() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swErr = new StringWriter();
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("use", "nonexistent-key-98765");
        
        assertEquals(1, exitCode);
        String errorOutput = swErr.toString();
        assertTrue(errorOutput.contains("Key not found"));
    }

    @Test
    void testUseCommandOutputFormat() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        // Test with a known key if it exists, otherwise we expect an error
        int exitCode = cmd.execute("use", "id_rsa");
        
        String output = swOut.toString();
        String errorOutput = swErr.toString();
        
        // Either it succeeds with proper output, or fails because key doesn't exist
        if (exitCode == 0) {
            // Check that output contains ssh-agent or ssh-add commands
            assertTrue(output.contains("ssh-agent") || output.contains("ssh-add"));
        } else {
            assertTrue(errorOutput.contains("Key not found") || 
                      errorOutput.contains("Not a valid private key"));
        }
    }

    @Test
    void testUseCommandQuietMode() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("use", "id_rsa", "--quiet");
        
        String output = swOut.toString();
        
        if (exitCode == 0) {
            // In quiet mode, should only output the command, no comments
            assertTrue(!output.contains("# ") || output.trim().split("\n").length == 1);
            assertTrue(output.contains("ssh-add") || output.contains("ssh-agent"));
        }
    }

    @Test
    void testUseCommandWithLifetime() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));

        int exitCode = cmd.execute("use", "id_rsa", "--time", "3600");
        
        if (exitCode == 0) {
            String output = swOut.toString();
            // Should include -t 3600 in the ssh-add command
            assertTrue(output.contains("-t 3600"));
        }
    }
}
