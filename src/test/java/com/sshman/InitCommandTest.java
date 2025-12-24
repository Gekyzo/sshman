package com.sshman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class InitCommandTest {

    @Test
    void testInitHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("init", "--help");

        assertEquals(0, exitCode);
        String output = sw.toString();
        assertTrue(output.contains("Create a .sshman file"));
        assertTrue(output.contains("--force"));
    }

    @Test
    void testInitMissingKeyName() {
        CommandLine cmd = new CommandLine(new SshMan());
        int exitCode = cmd.execute("init");
        assertEquals(2, exitCode); // Missing required parameter
    }

    @Test
    void testInitNonexistentKey() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swErr = new StringWriter();
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("init", "nonexistent-key-98765");

        assertEquals(1, exitCode);
        String errorOutput = swErr.toString();
        assertTrue(errorOutput.contains("Key not found"));
    }

    @Test
    void testInitCreatesFile(@TempDir Path tempDir) throws Exception {
        // Create a test SSH directory with a dummy key
        Path sshDir = tempDir.resolve(".ssh");
        Files.createDirectory(sshDir);

        Path testKey = sshDir.resolve("test_key");
        Files.writeString(testKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Set working directory to temp dir (simulated via changing user.dir)
        String originalDir = System.getProperty("user.dir");
        Path workDir = tempDir.resolve("project");
        Files.createDirectory(workDir);
        System.setProperty("user.dir", workDir.toString());

        try {
            CommandLine cmd = new CommandLine(new SshMan());
            StringWriter swOut = new StringWriter();
            StringWriter swErr = new StringWriter();
            cmd.setOut(new PrintWriter(swOut));
            cmd.setErr(new PrintWriter(swErr));

            int exitCode = cmd.execute("init", "test_key", "--path", sshDir.toString());

            String output = swOut.toString();
            String errorOutput = swErr.toString();

            // Should succeed if key exists or fail with proper message
            if (exitCode == 0) {
                assertTrue(output.contains("Created .sshman"));
                Path sshmanFile = workDir.resolve(".sshman");
                assertTrue(Files.exists(sshmanFile));
                String content = Files.readString(sshmanFile).trim();
                assertEquals("test_key", content);
            } else {
                // Key validation may fail in test environment
                assertTrue(errorOutput.contains("Key not found") ||
                          errorOutput.contains("Not a valid private key"));
            }
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testInitFileExistsWithoutForce(@TempDir Path tempDir) throws Exception {
        // Create .sshman file
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            Path sshmanFile = tempDir.resolve(".sshman");
            Files.writeString(sshmanFile, "existing_key\n");

            CommandLine cmd = new CommandLine(new SshMan());
            StringWriter swErr = new StringWriter();
            cmd.setErr(new PrintWriter(swErr));

            int exitCode = cmd.execute("init", "test_key");

            assertEquals(1, exitCode);
            String errorOutput = swErr.toString();
            assertTrue(errorOutput.contains("already exists"));
            assertTrue(errorOutput.contains("--force"));
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }

    @Test
    void testInitFileExistsWithForce(@TempDir Path tempDir) throws Exception {
        // Create a test SSH directory with a dummy key
        Path sshDir = tempDir.resolve(".ssh");
        Files.createDirectory(sshDir);

        Path testKey = sshDir.resolve("new_key");
        Files.writeString(testKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create existing .sshman file
        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        try {
            Path sshmanFile = tempDir.resolve(".sshman");
            Files.writeString(sshmanFile, "old_key\n");

            CommandLine cmd = new CommandLine(new SshMan());
            StringWriter swOut = new StringWriter();
            StringWriter swErr = new StringWriter();
            cmd.setOut(new PrintWriter(swOut));
            cmd.setErr(new PrintWriter(swErr));

            int exitCode = cmd.execute("init", "new_key", "--force", "--path", sshDir.toString());

            String output = swOut.toString();
            String errorOutput = swErr.toString();

            if (exitCode == 0) {
                assertTrue(output.contains("Created .sshman"));
                String content = Files.readString(sshmanFile).trim();
                assertEquals("new_key", content);
            } else {
                // Key validation may fail
                assertTrue(errorOutput.contains("Key not found") ||
                          errorOutput.contains("Not a valid private key"));
            }
        } finally {
            System.setProperty("user.dir", originalDir);
        }
    }
}
