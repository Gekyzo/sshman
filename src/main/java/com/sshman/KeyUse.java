package com.sshman;

/**
 * Defines the intended use/purpose of an SSH key.
 */
public enum KeyUse {

    WORK("work", "Work/Professional use"),
    PERSONAL("personal", "Personal use"),
    OTHER("other", "Other/Unspecified"),
    ;

    private final String value;
    private final String description;

    KeyUse(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse a string value to KeyUse.
     *
     * @param value the string value
     * @return the matching KeyUse or OTHER if not found
     */
    public static KeyUse fromString(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        for (KeyUse use : values()) {
            if (use.value.equalsIgnoreCase(value.trim())) {
                return use;
            }
        }
        return OTHER;
    }

    @Override
    public String toString() {
        return value;
    }
}
