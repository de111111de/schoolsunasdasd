package ru.schoolsun.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Эталон для типов NUMBER и TEXT */
    @Column(name = "answer_text", length = 500)
    private String answerText;

    @Column(nullable = false)
    private int points = 1;

    @Column(nullable = false)
    private int position;
}
