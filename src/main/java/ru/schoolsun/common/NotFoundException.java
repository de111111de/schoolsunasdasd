package ru.schoolsun.common;

/** Ошибка «не найдено» — превращается в страницу 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
