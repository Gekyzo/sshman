package com.sshman;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
    name = "sshman",
    mixinStandardHelpOptions = true,
    version = "sshman ${project.version}",  // Populated at build time
    description = "SSH Centralized CLI - Manage SSH keys and connections",
    subcommands = {
        GenerateCommand.class,  // Actual subcommand classes
        ListCommand.class,
        InfoCommand.class,
        UseCommand.class,
        InitCommand.class,
        CommandLine.HelpCommand.class
    },
    footer = "%nExamples:%n" +
        "  sshman generate --algo ed25519 --use work%n" +
        "  sshman list%n" +
        "  sshman info work-key%n" +
        "  eval \"$(sshman use id_ed25519 --quiet)\"%n" +
        "  sshman init work/project-key"
)
public class SshMan implements Callable<Integer> {

    @Spec
    CommandSpec spec;  // Injected by picocli

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SshMan())
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
}
