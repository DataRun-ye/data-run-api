package org.nmcpye.datarun.jpa.etl.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class DateUtils {

    // DateTimeFormatter for ISO 8601 formats
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static boolean dateIsValid(Object v) {
        final var dateStr = v.toString().trim();
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean dateTimeIsValid(Object v) {
        final var dateTimeStr = v.toString().trim();
        try {
            // Using ISO_OFFSET_DATE_TIME because the Flutter app sends 'Z' for UTC
            LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // You can also add methods to parse the values directly
    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null; // or throw a custom exception
        }
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null; // or throw a custom exception
        }
    }
}
