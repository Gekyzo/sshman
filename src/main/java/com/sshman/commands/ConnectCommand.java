package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.sshman.utils.printer.Text.*;

@Command(
    name = "connect",
    description = "Connect to a saved SSH profile",
    mixinStandardHelpOptions = true
)
public class ConnectCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

    @Parameters(
        index = "0",
        description = "Profile alias to connect to",
        completionCandidates = ProfileCompletionCandidates.class
    )
    private String alias;

    private final ProfileStorage storage = new ProfileStorage();

    private static final String HEADER_LINE = "═".repeat(65);

    @Override
    public Integer call() {
        Optional<Profile> profileOpt = storage.getProfile(alias);

        if (profileOpt.isEmpty()) {
            printer.error(red("Profile not found: "), bold(alias));
            printer.error("");
            printer.error(yellow("Available profiles:"));
            listProfiles();
            printer.error("");
            printer.error(gray("To create a new profile, run:"));
            printer.error("  ", cyan("sshman connect-new"));
            return 1;
        }

        Profile profile = profileOpt.get();
        String sshCommand = profile.toSshCommand();

        // Print header
        printer.println(bold(HEADER_LINE));
        printer.println(bold("  SSH Connection"));
        printer.println(bold(HEADER_LINE));
        printer.emptyLine();

        printer.println(label("Profile"), cyan(profile.getAlias()));
        printer.println(label("Hostname"), textOf(profile.getHostname()));
        printer.println(label("Username"), textOf(profile.getUsername()));
        if (profile.getPort() != null && profile.getPort() != 22) {
            printer.println(label("Port"), textOf(String.valueOf(profile.getPort())));
        }
        if (profile.getSshKey() != null && !profile.getSshKey().isEmpty()) {
            printer.println(label("SSH Key"), gray(profile.getSshKey()));
        }
        printer.emptyLine();

        printer.println(gray("Equivalent SSH command:"));
        printer.println("  ", textOf(sshCommand));
        printer.emptyLine();

        printer.println(green("→ "), textOf("Connecting to "), bold(profile.getAlias()), textOf("..."));
        printer.println(bold(HEADER_LINE));
        printer.emptyLine();

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("sh", "-c", sshCommand);
            pb.inheritIO();
            Process process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            printer.error(red("Failed to execute SSH command: "), textOf(e.getMessage()));
            return 1;
        }
    }

    private void listProfiles() {
        var profiles = storage.loadProfiles();
        if (profiles.isEmpty()) {
            printer.error("  ", gray("(no profiles found)"));
        } else {
            profiles.forEach(p ->
                printer.error("  ", cyan("- "), textOf(p.getAlias()), gray(" ("), textOf(p.getUsername() + "@" + p.getHostname()), gray(")"))
            );
        }
    }

    public static class ProfileCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            ProfileStorage storage = new ProfileStorage();
            return storage.loadProfiles().stream()
                .map(Profile::getAlias)
                .sorted()
                .iterator();
        }
    }
}
