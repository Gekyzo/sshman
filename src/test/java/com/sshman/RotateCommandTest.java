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

class RotateCommandTest {

    @Test
    void testRotateHelp() {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));

        int exitCode = cmd.execute("rotate", "--help");

        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("rotate"));
        assertTrue(sw.toString().contains("Rotate SSH key"));
    }

    @Test
    void testRotateNonExistentKey(@TempDir Path tempDir) {
        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "nonexistent_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Key not found") || swErr.toString().contains("not found"));
    }

    @Test
    void testRotateDryRun(@TempDir Path tempDir) throws IOException {
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

        int exitCode = cmd.execute("rotate", "test_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("DRY RUN"));
        assertTrue(swOut.toString().contains("Would perform"));

        // Verify original key still exists (dry run shouldn't modify)
        assertTrue(Files.exists(keyPath));
        assertTrue(Files.exists(pubKeyPath));
    }

    @Test
    void testRotateInvalidKeyType(@TempDir Path tempDir) throws IOException {
        // Create a mock private key
        Path keyPath = tempDir.resolve("test_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("test_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "test_key", "--path", tempDir.toString(), "--type", "invalid");

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Invalid key type"));
    }

    @Test
    void testRotateAlreadyArchivedKey(@TempDir Path tempDir) throws IOException {
        // Create archived directory and key
        Path archivedDir = tempDir.resolve("archived");
        Files.createDirectories(archivedDir);

        Path archivedKeyPath = archivedDir.resolve("archived_key");
        Files.writeString(archivedKeyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "archived/archived_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("already archived"));
    }

    @Test
    void testRotateNotPrivateKey(@TempDir Path tempDir) throws IOException {
        // Create a file that's not a private key
        Path keyPath = tempDir.resolve("not_a_key");
        Files.writeString(keyPath, "This is not a private key\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "not_a_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(1, exitCode);
        assertTrue(swErr.toString().contains("Not a valid private key"));
    }

    @Test
    void testRotateBatchKeys(@TempDir Path tempDir) throws IOException {
        // Create multiple mock private keys
        Path key1Path = tempDir.resolve("key1");
        Files.writeString(key1Path, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content 1\n-----END OPENSSH PRIVATE KEY-----\n");
        Files.writeString(tempDir.resolve("key1.pub"), "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test1@example.com\n");

        Path key2Path = tempDir.resolve("key2");
        Files.writeString(key2Path, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content 2\n-----END OPENSSH PRIVATE KEY-----\n");
        Files.writeString(tempDir.resolve("key2.pub"), "ssh-rsa AAAAB3NzaC1 test2@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "key1", "key2", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        String output = swOut.toString();
        assertTrue(output.contains("key1"));
        assertTrue(output.contains("key2"));
        assertTrue(output.contains("Total keys processed: 2"));
    }

    @Test
    void testRotateDetectsEd25519KeyType(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("ed_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("ed_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "ed_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("ed25519"));
    }

    @Test
    void testRotateDetectsRsaKeyType(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("rsa_key");
        Files.writeString(keyPath, "-----BEGIN RSA PRIVATE KEY-----\ntest content\n-----END RSA PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("rsa_key.pub");
        Files.writeString(pubKeyPath, "ssh-rsa AAAAB3NzaC1 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "rsa_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("rsa"));
    }

    @Test
    void testRotateWithCustomComment(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("test_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("test_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 old@comment\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "test_key", "--path", tempDir.toString(),
            "--comment", "new custom comment", "--dry-run");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("new custom comment"));
    }

    @Test
    void testRotatePreservesOriginalComment(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("test_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("test_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 original@comment\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "test_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        assertTrue(swOut.toString().contains("original@comment"));
    }

    @Test
    void testRotateWithTypeChange(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("rsa_key");
        Files.writeString(keyPath, "-----BEGIN RSA PRIVATE KEY-----\ntest content\n-----END RSA PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("rsa_key.pub");
        Files.writeString(pubKeyPath, "ssh-rsa AAAAB3NzaC1 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "rsa_key", "--path", tempDir.toString(),
            "--type", "ed25519", "--dry-run");

        assertEquals(0, exitCode);
        String output = swOut.toString();
        assertTrue(output.contains("Original key type: rsa"));
        assertTrue(output.contains("New key type: ed25519"));
    }

    @Test
    void testRotateShowsSummary(@TempDir Path tempDir) throws IOException {
        Path keyPath = tempDir.resolve("test_key");
        Files.writeString(keyPath, "-----BEGIN OPENSSH PRIVATE KEY-----\ntest content\n-----END OPENSSH PRIVATE KEY-----\n");

        Path pubKeyPath = tempDir.resolve("test_key.pub");
        Files.writeString(pubKeyPath, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5 test@example.com\n");

        CommandLine cmd = new CommandLine(new SshMan());
        StringWriter swOut = new StringWriter();
        StringWriter swErr = new StringWriter();
        cmd.setOut(new PrintWriter(swOut));
        cmd.setErr(new PrintWriter(swErr));

        int exitCode = cmd.execute("rotate", "test_key", "--path", tempDir.toString(), "--dry-run");

        assertEquals(0, exitCode);
        String output = swOut.toString();
        assertTrue(output.contains("Rotation Summary"));
        assertTrue(output.contains("Total keys processed"));
        assertTrue(output.contains("Successful"));
    }
}
