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

class ArchiveCommandTest {

    @Test
    void testArchiveHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("archive", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("archive"));
        assertTrue(sw.toString().contains("Archive an SSH key"));
    }

    @Test
    void testArchiveNonExistentKey(@TempDir Path tempDir) {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "nonexistent_key", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Key not found"));
    }

    @Test
    void testArchiveInvalidPath() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "somekey", "--path", "/nonexistent/invalid/path");

        assertEquals(1, exitCode);
    }

    @Test
    void testArchivePrivateKey(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("test_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock public key
        Path pubKeyPath = tempDir.resolve("test_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "test_key", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Archived"));
        assertTrue(swOut.toString().contains("successfully"));

        // Verify the key was moved to archived directory
        Path archivedKey = tempDir.resolve("archived/test_key");
        Path archivedPub = tempDir.resolve("archived/test_key.pub");
        assertTrue(Files.exists(archivedKey), "Archived private key should exist");
        assertTrue(Files.exists(archivedPub), "Archived public key should exist");

        // Verify original files were removed
        assertFalse(Files.exists(keyPath), "Original private key should be removed");
        assertFalse(Files.exists(pubKeyPath), "Original public key should be removed");
    }

    @Test
    void testArchiveKeyWithoutPublicKey(@TempDir Path tempDir) throws IOException {
        // Create a mock private key without a public key
        Path keyPath = tempDir.resolve("test_key_no_pub");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "test_key_no_pub", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Archived"));

        // Verify the key was moved to archived directory
        Path archivedKey = tempDir.resolve("archived/test_key_no_pub");
        assertTrue(Files.exists(archivedKey), "Archived private key should exist");

        // Verify original file was removed
        assertFalse(Files.exists(keyPath), "Original private key should be removed");
    }

    @Test
    void testArchiveKeyInSubdirectory(@TempDir Path tempDir) throws IOException {
        // Create a subdirectory structure
        Path workDir = tempDir.resolve("work");
        Files.createDirectories(workDir);

        // Create a mock private key in subdirectory
        Path keyPath = workDir.resolve("id_work_ed25519");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock public key
        Path pubKeyPath = workDir.resolve("id_work_ed25519.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 work@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "work/id_work_ed25519", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("Archived"));

        // Verify the key was moved to archived directory with subdirectory structure
        Path archivedKey = tempDir.resolve("archived/work/id_work_ed25519");
        Path archivedPub = tempDir.resolve("archived/work/id_work_ed25519.pub");
        assertTrue(Files.exists(archivedKey), "Archived private key should exist in subdirectory");
        assertTrue(Files.exists(archivedPub), "Archived public key should exist in subdirectory");

        // Verify original files were removed
        assertFalse(Files.exists(keyPath), "Original private key should be removed");
        assertFalse(Files.exists(pubKeyPath), "Original public key should be removed");

        // Verify empty work directory was cleaned up
        assertFalse(Files.exists(workDir), "Empty work directory should be removed");
    }

    @Test
    void testArchiveAlreadyArchivedKey(@TempDir Path tempDir) throws IOException {
        // Create archived directory with a key
        Path archivedDir = tempDir.resolve("archived");
        Files.createDirectories(archivedDir);

        Path archivedKey = archivedDir.resolve("old_key");
        Files.writeString(archivedKey, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "archived/old_key", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("already archived"));
    }

    @Test
    void testArchiveNonPrivateKeyFile(@TempDir Path tempDir) throws IOException {
        // Create a non-key file
        Path nonKeyPath = tempDir.resolve("not_a_key.txt");
        Files.writeString(nonKeyPath, "This is just a text file");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("archive", "not_a_key.txt", "--path", tempDir.toString());

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Not a valid private key"));
    }

    @Test
    void testArchiveKeyUsedInConfigWithForce(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("id_production");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock SSH config that references the key
        Path configPath = tempDir.resolve("config");
        Files.writeString(configPath,
            "Host production\n" +
            "  HostName prod.example.com\n" +
            "  User admin\n" +
            "  IdentityFile " + keyPath.toString() + "\n"
        );

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        // Archive with --force flag should succeed without prompting
        int exitCode = cmd.execute("archive", "id_production", "--path", tempDir.toString(), "--force");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("successfully archived"));
        assertTrue(swOut.toString().contains("Warning: Archived key is still referenced in ~/.ssh/config"));
        assertTrue(swOut.toString().contains("production"));

        // Verify key was archived
        Path archivedKey = tempDir.resolve("archived/id_production");
        assertTrue(Files.exists(archivedKey));
        assertFalse(Files.exists(keyPath));
    }

    @Test
    void testArchiveKeyNotUsedInConfig(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("id_unused");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock SSH config that references a different key
        Path configPath = tempDir.resolve("config");
        Files.writeString(configPath,
            "Host production\n" +
            "  HostName prod.example.com\n" +
            "  User admin\n" +
            "  IdentityFile ~/.ssh/id_rsa\n"
        );

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        // Should archive without confirmation since key is not in use
        int exitCode = cmd.execute("archive", "id_unused", "--path", tempDir.toString());

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("successfully archived"));
        assertFalse(swOut.toString().contains("Warning: Archived key is still referenced"));

        // Verify key was archived
        Path archivedKey = tempDir.resolve("archived/id_unused");
        assertTrue(Files.exists(archivedKey));
        assertFalse(Files.exists(keyPath));
    }

    @Test
    void testArchiveKeyUsedInMultipleHosts(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("id_shared");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock SSH config with multiple hosts using the same key
        Path configPath = tempDir.resolve("config");
        Files.writeString(configPath,
            "Host server1\n" +
            "  HostName server1.example.com\n" +
            "  IdentityFile " + keyPath.toString() + "\n" +
            "\n" +
            "Host server2\n" +
            "  HostName server2.example.com\n" +
            "  IdentityFile " + keyPath.toString() + "\n" +
            "\n" +
            "Host server3\n" +
            "  HostName server3.example.com\n" +
            "  IdentityFile ~/.ssh/other_key\n"
        );

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        // Archive with --force flag
        int exitCode = cmd.execute("archive", "id_shared", "--path", tempDir.toString(), "--force");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("successfully archived"));
        assertTrue(swOut.toString().contains("Warning: Archived key is still referenced"));
        assertTrue(swOut.toString().contains("server1"));
        assertTrue(swOut.toString().contains("server2"));
        assertFalse(swOut.toString().contains("server3"));

        // Verify key was archived
        Path archivedKey = tempDir.resolve("archived/id_shared");
        assertTrue(Files.exists(archivedKey));
        assertFalse(Files.exists(keyPath));
    }

    @Test
    void testArchiveKeyWithRelativePathInConfig(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("id_relative");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        // Create a mock SSH config with relative path
        Path configPath = tempDir.resolve("config");
        Files.writeString(configPath,
            "Host myserver\n" +
            "  HostName myserver.example.com\n" +
            "  IdentityFile id_relative\n"
        );

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        // Archive with --force flag
        int exitCode = cmd.execute("archive", "id_relative", "--path", tempDir.toString(), "-f");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("successfully archived"));
        assertTrue(swOut.toString().contains("Warning: Archived key is still referenced"));
        assertTrue(swOut.toString().contains("myserver"));

        // Verify key was archived
        Path archivedKey = tempDir.resolve("archived/id_relative");
        assertTrue(Files.exists(archivedKey));
        assertFalse(Files.exists(keyPath));
    }
}
