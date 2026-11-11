package ru.schoolsun.course;

import lombok.Data;

@Data
public class CourseForm {

    private String title;
    private String description;
    private String subject = "MATH";
    private Integer grade = 8;
    /** sun / sky / ember — цветовой акцент карточки */
    private String accent = "sun";
    private boolean sequentialMode;
    private boolean published;
    /** Спецкурс: доступ только по ссылке-приглашению */
    private boolean inviteOnly;
}
