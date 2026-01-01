package com.sshman.utils.printer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;

@Command
public class Printer {

    @Spec
    private CommandSpec spec;

    public PrintWriter out() {
        return spec.commandLine().getOut();
    }

    public PrintWriter err() {
        return spec.commandLine().getErr();
    }

    // Fluent API entry point
    public PrintBuilder print() {
        return new PrintBuilder(out());
    }

    // Direct convenience methods (no spacing)
    public void print(Object... parts) {
        out().print(join(parts));
    }

    public void printf(String format, Object... args) {
        out().printf(format, args);
    }

    public void println(Object... parts) {
        out().println(join(parts));
    }

    public void emptyLine() {
        out().println();
    }

    public void emptyLines(int count) {
        for (int i = 0; i < count; i++) {
            out().println();
        }
    }

    // Print to stderr
    public void error(Object... parts) {
        err().println(join(parts));
    }

    // Helper to join multiple parts
    private static String join(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            sb.append(part != null ? part.toString() : "");
        }
        return sb.toString();
    }

    public static class PrintBuilder {

        private final PrintWriter out;
        private int linesBefore = 0;
        private int linesAfter = 0;

        private PrintBuilder(PrintWriter out) {
            this.out = out;
        }

        // Spacing: before
        public PrintBuilder before() {
            this.linesBefore = 1;
            return this;
        }

        public PrintBuilder before(int lines) {
            this.linesBefore = lines;
            return this;
        }

        // Spacing: after
        public PrintBuilder after() {
            this.linesAfter = 1;
            return this;
        }

        public PrintBuilder after(int lines) {
            this.linesAfter = lines;
            return this;
        }

        // Spacing: both
        public PrintBuilder padded() {
            this.linesBefore = 1;
            this.linesAfter = 1;
            return this;
        }

        public PrintBuilder padded(int lines) {
            this.linesBefore = lines;
            this.linesAfter = lines;
            return this;
        }

        // Terminal: println
        public void ln(Object... parts) {
            emptyLines(linesBefore);
            out.println(join(parts));
            emptyLines(linesAfter);
        }

        // Terminal: printf
        public void f(String format, Object... args) {
            emptyLines(linesBefore);
            out.printf(format, args);
            emptyLines(linesAfter);
        }

        // Terminal: print (no newline)
        public void text(Object... parts) {
            emptyLines(linesBefore);
            out.print(join(parts));
            emptyLines(linesAfter);
        }

        private void emptyLines(int count) {
            for (int i = 0; i < count; i++) {
                out.println();
            }
        }
    }

    /**
     * Flushes the output stream.
     */
    public void flush() {
        out().flush();
    }

    /**
     * Prints without newline and flushes immediately.
     * Useful for prompts that expect user input.
     */
    public void prompt(Object... parts) {
        out().print(join(parts));
        out().flush();
    }
}
