package ru.schoolsun.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quizzes")
@Getter
@Setter
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", unique = true)
    private ru.schoolsun.lesson.Lesson lesson;

    @Column(nullable = false, length = 190)
    private String title;

    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "show_answers", nullable = false)
    private boolean showAnswers = true;
}
