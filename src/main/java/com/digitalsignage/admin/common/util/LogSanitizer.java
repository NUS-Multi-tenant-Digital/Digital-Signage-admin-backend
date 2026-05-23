package com.digitalsignage.admin.common.util;

/**
 * Strips control characters from values written to logs to avoid log injection.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\t' || c < 0x20) {
                out.append('_');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
