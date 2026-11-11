package ru.schoolsun.course;

public enum Subject {
    MATH("Математика"),
    PHYSICS("Физика"),
    CS("Информатика"),
    CHEMISTRY("Химия"),
    BIOLOGY("Биология"),
    HISTORY("История"),
    LANG("Иностранный язык"),
    OTHER("Другое");

    private final String label;

    Subject(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
