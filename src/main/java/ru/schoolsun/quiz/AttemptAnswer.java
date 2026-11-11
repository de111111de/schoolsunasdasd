package ru.schoolsun.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "answers")
@Getter
@Setter
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    /** JSON/строковое представление ответа ученика */
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "earned_points", nullable = false)
    private int earnedPoints;
}
