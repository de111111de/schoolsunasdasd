package ru.schoolsun.quiz;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "options")
@Getter
@Setter
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    /** Правая пара для типа MATCH */
    @Column(name = "pair_key", length = 500)
    private String pairKey;
}
