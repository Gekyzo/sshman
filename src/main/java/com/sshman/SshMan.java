package com.sshman;

import com.sshman.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

@Command(
    name = "sshman",
    mixinStandardHelpOptions = true,
    versionProvider = SshMan.VersionProvider.class,
    description = "SSH Centralized CLI - Manage SSH keys and connections",
    subcommands = {
        GenerateCommand.class,  // Actual subcommand classes
        ListCommand.class,
        InfoCommand.class,
        UseCommand.class,
        InitCommand.class,
        ConnectNewCommand.class,
        ConnectCommand.class,
        ListProfilesCommand.class,
        ArchiveCommand.class,
        UnarchiveCommand.class,
        RotateCommand.class,
        CommandLine.HelpCommand.class
    },
    footer = """
        %nExamples:
          sshman generate --algo ed25519 --use work
          sshman list
          sshman info work-key
          eval "$(sshman use id_ed25519 --quiet)"
          sshman init work/project-key
          sshman connect-new
          sshman connect myserver"""
)
public class SshMan implements Callable<Integer> {

    @Spec
    CommandSpec spec;  // Injected by picocli

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SshMan(), new SshManFactory())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setExecutionExceptionHandler(new ExceptionHandler())
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        // Use picocli's built-in usage instead of hardcoded text
        spec.commandLine().usage(System.out);
        return 0;
    }

    // Custom exception handler for better UX
    private static class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception ex, CommandLine cmd,
                                            CommandLine.ParseResult parseResult) {
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
            return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();
        }
    }

    // Version provider that reads from version.properties
    static class VersionProvider implements IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            Properties props = new Properties();
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("version.properties")) {
                if (is == null) {
                    return new String[]{"sshman (version unknown)"};
                }
                props.load(is);
                String version = props.getProperty("version", "unknown");
                return new String[]{"sshman " + version};
            }
        }
    }
}
