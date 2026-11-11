package ru.schoolsun.homework;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class HomeworkForm {

    private String title;
    private String instruction;
    private Integer maxPoints = 10;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadline;
}
