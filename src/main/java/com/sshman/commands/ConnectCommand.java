package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.ProfileStorageAware;
import com.sshman.ProfileStorageProvider;
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
    aliases = {"c"},
    description = "Connect to a saved SSH profile",
    mixinStandardHelpOptions = true
)
public class ConnectCommand implements Callable<Integer>, ProfileStorageAware {

    @Mixin
    private Printer printer;

    @Parameters(
        index = "0",
        description = "Profile alias to connect to",
        completionCandidates = ProfileCompletionCandidates.class
    )
    private String alias;

    private ProfileStorageProvider storageProvider = ProfileStorageProvider.DEFAULT;

    private static final String HEADER_LINE = "═".repeat(65);

    @Override
    public void setProfileStorageProvider(ProfileStorageProvider provider) {
        this.storageProvider = provider;
    }

    @Override
    public Integer call() {
        ProfileStorage storage = storageProvider.get();
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

        printer.println(label("Profile"), cyan(profile.alias()));
        printer.println(label("Hostname"), textOf(profile.hostname()));
        printer.println(label("Username"), textOf(profile.username()));
        if (profile.port() != null && profile.port() != 22) {
            printer.println(label("Port"), textOf(String.valueOf(profile.port())));
        }
        if (profile.sshKey() != null && !profile.sshKey().isEmpty()) {
            printer.println(label("SSH Key"), gray(profile.sshKey()));
        }
        printer.emptyLine();

        printer.println(gray("Equivalent SSH command:"));
        printer.println("  ", textOf(sshCommand));
        printer.emptyLine();

        printer.println(green("→ "), textOf("Connecting to "), bold(profile.alias()), textOf("..."));
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
        var profiles = storageProvider.get().loadProfiles();
        if (profiles.isEmpty()) {
            printer.error("  ", gray("(no profiles found)"));
        } else {
            profiles.forEach(p ->
                printer.error("  ", cyan("- "), textOf(p.alias()), gray(" ("), textOf(p.username() + "@" + p.hostname()), gray(")"))
            );
        }
    }

    public static class ProfileCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            ProfileStorage storage = new ProfileStorage();
            return storage.loadProfiles().stream()
                .map(Profile::alias)
                .sorted()
                .iterator();
        }
    }
}
