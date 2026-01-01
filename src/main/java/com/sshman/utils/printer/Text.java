package com.sshman.utils.printer;

import picocli.CommandLine.Help.Ansi;

import java.util.Objects;

/**
 * Immutable styled text wrapper for ANSI-colored console output.
 *
 * <p>Provides a fluent API for creating styled text using picocli's ANSI support.
 * Styles can be combined for rich console output.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple colors
 * Text.red("Error: %s", message);
 * Text.green("Success!");
 *
 * // Styles
 * Text.bold("Important");
 * Text.italic("Note");
 *
 * // Combined styles
 * Text.styled("bold,red", "Critical Error");
 * Text.red("Error").bold();
 *
 * // Chained styles
 * Text.of("Warning").yellow().bold().underline();
 * }</pre>
 *
 * @author sshman
 * @version 1.0
 * @since 1.0
 */
public record Text(

    String content

) {

    private static final Ansi ANSI = Ansi.AUTO;

    /**
     * Canonical constructor with null validation.
     *
     * @param content the text content (cannot be null)
     */
    public Text {
        Objects.requireNonNull(content, "content cannot be null");
    }

    // ========================================================================
    // Static Factory Methods - Colors
    // ========================================================================

    /**
     * Creates plain text without any styling.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new Text instance
     */
    public static Text textOf(String format, Object... args) {
        return new Text(format(format, args));
    }

    /**
     * Creates gray (dimmed) text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text gray(String format, Object... args) {
        return styled("fg(245)", format, args);
    }

    /**
     * Creates red text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text red(String format, Object... args) {
        return styled("red", format, args);
    }

    /**
     * Creates green text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text green(String format, Object... args) {
        return styled("green", format, args);
    }

    /**
     * Creates yellow text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text yellow(String format, Object... args) {
        return styled("yellow", format, args);
    }

    /**
     * Creates blue text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text blue(String format, Object... args) {
        return styled("blue", format, args);
    }

    /**
     * Creates cyan text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text cyan(String format, Object... args) {
        return styled("cyan", format, args);
    }

    /**
     * Creates magenta text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text magenta(String format, Object... args) {
        return styled("magenta", format, args);
    }

    /**
     * Creates white text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text white(String format, Object... args) {
        return styled("white", format, args);
    }

    // ========================================================================
    // Static Factory Methods - Styles
    // ========================================================================

    /**
     * Creates bold text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text bold(String format, Object... args) {
        return styled("bold", format, args);
    }

    /**
     * Creates italic text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text italic(String format, Object... args) {
        return styled("italic", format, args);
    }

    /**
     * Creates underlined text.
     *
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text underline(String format, Object... args) {
        return styled("underline", format, args);
    }

    /**
     * Creates text with custom style(s).
     *
     * <p>Multiple styles can be combined with commas:</p>
     * <pre>{@code
     * Text.styled("bold,red", "Error");
     * Text.styled("underline,yellow", "Warning: %s", msg);
     * }</pre>
     *
     * @param styles comma-separated style names
     * @param format format string or plain text
     * @param args   optional format arguments
     * @return new styled Text instance
     */
    public static Text styled(String styles, String format, Object... args) {
        String text = format(format, args);
        return new Text(ANSI.string("@|" + styles + " " + text + "|@"));
    }

    // ========================================================================
    // Instance Methods - Chainable Style Modifiers
    // ========================================================================

    /**
     * Applies bold style to this text.
     *
     * @return new Text instance with bold applied
     */
    public Text bold() {
        return applyStyle("bold");
    }

    /**
     * Applies italic style to this text.
     *
     * @return new Text instance with italic applied
     */
    public Text italic() {
        return applyStyle("italic");
    }

    /**
     * Applies underline style to this text.
     *
     * @return new Text instance with underline applied
     */
    public Text underline() {
        return applyStyle("underline");
    }

    /**
     * Applies red color to this text.
     *
     * @return new Text instance with red color applied
     */
    public Text red() {
        return applyStyle("red");
    }

    /**
     * Applies green color to this text.
     *
     * @return new Text instance with green color applied
     */
    public Text green() {
        return applyStyle("green");
    }

    /**
     * Applies yellow color to this text.
     *
     * @return new Text instance with yellow color applied
     */
    public Text yellow() {
        return applyStyle("yellow");
    }

    /**
     * Applies blue color to this text.
     *
     * @return new Text instance with blue color applied
     */
    public Text blue() {
        return applyStyle("blue");
    }

    /**
     * Applies gray color to this text.
     *
     * @return new Text instance with gray color applied
     */
    public Text gray() {
        return applyStyle("fg(245)");
    }

    /**
     * Applies a custom style to this text.
     *
     * @param style the style to apply
     * @return new Text instance with style applied
     */
    public Text style(String style) {
        return applyStyle(style);
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Checks if this text is empty.
     *
     * @return true if content is empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Checks if this text is blank (empty or whitespace only).
     *
     * @return true if content is blank
     */
    public boolean isBlank() {
        return content.isBlank();
    }

    /**
     * Returns the length of the content.
     *
     * @return content length
     */
    public int length() {
        return content.length();
    }

    /**
     * Concatenates this text with another.
     *
     * @param other the text to append
     * @return new Text instance with combined content
     */
    public Text append(Text other) {
        return new Text(this.content + other.content);
    }

    /**
     * Concatenates this text with a string.
     *
     * @param other the string to append
     * @return new Text instance with combined content
     */
    public Text append(String other) {
        return new Text(this.content + other);
    }

    @Override
    public String toString() {
        return content;
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    private Text applyStyle(String style) {
        return new Text(ANSI.string("@|" + style + " " + content + "|@"));
    }

    private static String format(String format, Object... args) {
        if (args == null || args.length == 0) {
            return format;
        }
        return String.format(format, args);
    }

    /**
     * Creates a formatted label with consistent width.
     *
     * @param name the label name
     * @return formatted label Text
     */
    public static Text label(String name) {
        return gray(String.format("%-14s", name + ":"));
    }
}
