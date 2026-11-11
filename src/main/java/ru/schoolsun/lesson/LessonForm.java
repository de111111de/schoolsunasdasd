package ru.schoolsun.lesson;

import lombok.Data;

@Data
public class LessonForm {

    private String title;
    private String theoryMd;
    private String videoUrl;
    private boolean published;
}
