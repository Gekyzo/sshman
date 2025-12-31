package com.sshman;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UnarchiveCommandTest {

    @Test
    void testUnarchiveHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("unarchive", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("unarchive"));
        assertTrue(sw.toString().contains("Restore an archived SSH key"));
    }

    @Test
    void testUnarchiveNonExistentKey(@TempDir Path tempDir) throws IOException {
        // Create archived directory but no keys
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "nonexistent_key", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Archived key not found"));
    }

    @Test
    void testUnarchiveWithoutArchivedDirectory(@TempDir Path tempDir) {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "somekey", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Archive directory does not exist"));
    }

    @Test
    void testUnarchivePrivateKey(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a key
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        Path archivedKey = archiveDir.resolve("test_key");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path archivedPub = archiveDir.resolve("test_key.pub");
        Files.writeString(archivedPub, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "test_key", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Restored"));
        assertTrue(swOut.toString().contains("successfully"));

        // Verify the key was moved from archived directory
        Path restoredKey = tempDir.resolve("test_key");
        Path restoredPub = tempDir.resolve("test_key.pub");
        assertTrue(Files.exists(restoredKey), "Restored private key should exist");
        assertTrue(Files.exists(restoredPub), "Restored public key should exist");

        // Verify archived files were removed
        assertFalse(Files.exists(archivedKey), "Archived private key should be removed");
        assertFalse(Files.exists(archivedPub), "Archived public key should be removed");
    }

    @Test
    void testUnarchiveKeyWithoutPublicKey(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a key (no public key)
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        Path archivedKey = archiveDir.resolve("test_key_no_pub");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "test_key_no_pub", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Restored"));

        // Verify the key was moved from archived directory
        Path restoredKey = tempDir.resolve("test_key_no_pub");
        assertTrue(Files.exists(restoredKey), "Restored private key should exist");

        // Verify archived file was removed
        assertFalse(Files.exists(archivedKey), "Archived private key should be removed");
    }

    @Test
    void testUnarchiveKeyInSubdirectory(@TempDir Path tempDir) throws IOException {
        // Create archived subdirectory structure
        Path archiveDir = tempDir.resolve("archived");
        Path archiveWorkDir = archiveDir.resolve("work");
        Files.createDirectories(archiveWorkDir);

        // Create a mock private key in archived subdirectory
        Path archivedKey = archiveWorkDir.resolve("id_work_ed25519");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path archivedPub = archiveWorkDir.resolve("id_work_ed25519.pub");
        Files.writeString(archivedPub, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 work@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "work/id_work_ed25519", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Restored"));

        // Verify the key was moved with subdirectory structure
        Path restoredKey = tempDir.resolve("work/id_work_ed25519");
        Path restoredPub = tempDir.resolve("work/id_work_ed25519.pub");
        assertTrue(Files.exists(restoredKey), "Restored private key should exist in subdirectory");
        assertTrue(Files.exists(restoredPub), "Restored public key should exist in subdirectory");

        // Verify archived files were removed
        assertFalse(Files.exists(archivedKey), "Archived private key should be removed");
        assertFalse(Files.exists(archivedPub), "Archived public key should be removed");

        // Verify empty archive work directory was cleaned up
        assertFalse(Files.exists(archiveWorkDir), "Empty archive work directory should be removed");
    }

    @Test
    void testUnarchiveKeyAlreadyExists(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a key
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        Path archivedKey = archiveDir.resolve("test_key");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\narchived content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create an existing key at target location
        Path existingKey = tempDir.resolve("test_key");
        Files.writeString(existingKey, "-----BEGIN OPENSSH PRIVATE KEY-----\nexisting content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "test_key", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Key already exists"));
        assertTrue(swErr.toString().contains("--force"));

        // Verify the existing key was not overwritten
        String content = Files.readString(existingKey);
        assertTrue(content.contains("existing content"), "Existing key should not be overwritten");
    }

    @Test
    void testUnarchiveKeyWithForceFlag(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a key
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        Path archivedKey = archiveDir.resolve("test_key");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\narchived content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create an existing key at target location
        Path existingKey = tempDir.resolve("test_key");
        Files.writeString(existingKey, "-----BEGIN OPENSSH PRIVATE KEY-----\nexisting content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "test_key", "--force", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Restored"));

        // Verify the existing key was overwritten with archived content
        String content = Files.readString(existingKey);
        assertTrue(content.contains("archived content"), "Existing key should be overwritten");
    }

    @Test
    void testUnarchiveNonPrivateKeyFile(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a non-key file
        Path archiveDir = tempDir.resolve("archived");
        Files.createDirectories(archiveDir);

        Path nonKeyPath = archiveDir.resolve("not_a_key.txt");
        Files.writeString(nonKeyPath, "This is just a text file");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "not_a_key.txt", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Not a valid private key"));
    }

    @Test
    void testUnarchiveInvalidPath() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("unarchive", "somekey", "--path", "/nonexistent/invalid/path");

        assertEquals(1, exitCode);
    }
}
