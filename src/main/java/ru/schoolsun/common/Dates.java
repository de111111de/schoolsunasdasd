package ru.schoolsun.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Форматирование дат для шаблонов Thymeleaf: ${T(ru.schoolsun.common.Dates).fmt(hw.deadline)} */
public final class Dates {

    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Dates() {
    }

    /** «28.10.2026 14:05» */
    public static String fmt(LocalDateTime dt) {
        return dt == null ? "" : dt.format(FULL);
    }

    /** «28.10.2026» */
    public static String date(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DAY);
    }

    /** для value у input[type=datetime-local] */
    public static String iso(LocalDateTime dt) {
        return dt == null ? "" : dt.format(ISO);
    }
}
