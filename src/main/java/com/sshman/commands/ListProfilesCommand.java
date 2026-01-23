package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.ProfileStorageAware;
import com.sshman.ProfileStorageProvider;
import com.sshman.constants.SshManConstants.ColumnWidths;
import com.sshman.utils.Strings;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

import static com.sshman.utils.printer.Text.*;

@Command(
    name = "list-profiles",
    aliases = {"profiles", "lp"},
    description = "List all saved SSH connection profiles",
    mixinStandardHelpOptions = true
)
public class ListProfilesCommand implements Callable<Integer>, ProfileStorageAware {

    @Mixin
    private Printer printer;

    @Option(names = {"-l", "--long"}, description = "Show detailed information in table format")
    private boolean longFormat;

    private ProfileStorageProvider storageProvider = ProfileStorageProvider.DEFAULT;

    @Override
    public void setProfileStorageProvider(ProfileStorageProvider provider) {
        this.storageProvider = provider;
    }

    @Override
    public Integer call() {
        List<Profile> profiles = storageProvider.get().loadProfiles();

        if (profiles.isEmpty()) {
            printEmptyMessage();
            return 0;
        }

        if (longFormat) {
            printLongFormat(profiles);
        } else {
            printShortFormat(profiles);
        }

        return 0;
    }

    private void printShortFormat(List<Profile> profiles) {
        printer.println("SSH Connection Profiles:");
        printer.emptyLine();

        profiles.forEach(p ->
            printer.println("  ", cyan("- "), cyan(p.alias()),
                gray(" ("), textOf(p.username() + "@" + p.hostname()), gray(")"))
        );

        printer.emptyLine();
        printer.printf("Total: %d profile(s)%n", profiles.size());
    }

    private void printLongFormat(List<Profile> profiles) {
        printer.println("SSH Connection Profiles:");
        printer.emptyLine();

        String headerFormat = "  %-" + ColumnWidths.ALIAS + "s %-" + ColumnWidths.HOSTNAME + "s %-" +
            ColumnWidths.USERNAME + "s %-" + ColumnWidths.PORT + "s %s%n";
        printer.printf(headerFormat, "ALIAS", "HOSTNAME", "USERNAME", "PORT", "SSH KEY");
        printer.println("  " + "-".repeat(ColumnWidths.TABLE_LINE));

        String rowFormat = "  %-" + ColumnWidths.ALIAS + "s %-" + ColumnWidths.HOSTNAME + "s %-" +
            ColumnWidths.USERNAME + "s %-" + ColumnWidths.PORT + "d %s%n";

        profiles.forEach(p -> {
            String sshKey = (p.sshKey() == null || p.sshKey().isEmpty())
                ? "-"
                : Strings.truncate(p.sshKey(), ColumnWidths.SSH_KEY_TRUNCATE);
            Integer port = p.port() != null ? p.port() : 22;

            printer.printf(rowFormat,
                Strings.truncate(p.alias(), ColumnWidths.ALIAS),
                Strings.truncate(p.hostname(), ColumnWidths.HOSTNAME),
                Strings.truncate(p.username(), ColumnWidths.USERNAME),
                port,
                sshKey
            );
        });

        printer.emptyLine();
        printer.printf("Total: %d profile(s)%n", profiles.size());
    }

    private void printEmptyMessage() {
        printer.println("No SSH connection profiles found");
        printer.emptyLine();
        printer.println(gray("To create a new profile, run:"));
        printer.println("  ", cyan("sshman connect-new"));
    }
}
