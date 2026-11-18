package org.nmcpye.datarun.common;

/**
 * Converts Arabic-Indic (٠-٩) and Extended Arabic-Indic (۰-۹) digits to 0-9.
 * Field phones with an Arabic keyboard type the former, while logins and
 * passwords are stored with Latin digits, so credentials are compared after
 * this conversion.
 */
public final class LatinDigits {

    private LatinDigits() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder converted = null;
        for (int index = 0; index < value.length(); index++) {
            char original = value.charAt(index);
            char latin = toLatin(original);
            if (latin != original) {
                if (converted == null) {
                    converted = new StringBuilder(value);
                }
                converted.setCharAt(index, latin);
            }
        }
        return converted == null ? value : converted.toString();
    }

    private static char toLatin(char character) {
        if (character >= '٠' && character <= '٩') {
            return (char) ('0' + (character - '٠'));
        }
        if (character >= '۰' && character <= '۹') {
            return (char) ('0' + (character - '۰'));
        }
        return character;
    }
}
