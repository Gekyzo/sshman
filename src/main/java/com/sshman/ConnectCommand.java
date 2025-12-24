package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "connect",
    description = "Connect to a saved SSH profile",
    mixinStandardHelpOptions = true
)
public class ConnectCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "Profile alias to connect to",
        completionCandidates = ProfileCompletionCandidates.class
    )
    private String alias;

    private final ProfileStorage storage = new ProfileStorage();

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        Optional<Profile> profileOpt = storage.getProfile(alias);

        if (profileOpt.isEmpty()) {
            err.println("Profile not found: " + alias);
            err.println();
            err.println("Available profiles:");
            listProfiles(err);
            err.println();
            err.println("To create a new profile, run:");
            err.println("  sshman connect-new");
            return 1;
        }

        Profile profile = profileOpt.get();
        String sshCommand = profile.toSshCommand();

        out.println("Connecting to " + profile.getAlias() + "...");
        out.println("Command: " + sshCommand);
        out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("sh", "-c", sshCommand);
            pb.inheritIO();
            Process process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            err.println("Failed to execute SSH command: " + e.getMessage());
            return 1;
        }
    }

    private void listProfiles(PrintWriter err) {
        var profiles = storage.loadProfiles();
        if (profiles.isEmpty()) {
            err.println("  (no profiles found)");
        } else {
            profiles.forEach(p ->
                err.println("  - " + p.getAlias() + " (" + p.getUsername() + "@" + p.getHostname() + ")")
            );
        }
    }

    public static class ProfileCompletionCandidates implements Iterable<String> {
        @Override
        public java.util.Iterator<String> iterator() {
            ProfileStorage storage = new ProfileStorage();
            return storage.loadProfiles().stream()
                .map(Profile::getAlias)
                .sorted()
                .iterator();
        }
    }
}
