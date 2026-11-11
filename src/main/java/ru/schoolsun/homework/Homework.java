package ru.schoolsun.homework;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "homeworks")
@Getter
@Setter
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", unique = true)
    private ru.schoolsun.lesson.Lesson lesson;

    @Column(nullable = false, length = 190)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "max_points", nullable = false)
    private int maxPoints = 10;

    @Column
    private LocalDateTime deadline;
}
