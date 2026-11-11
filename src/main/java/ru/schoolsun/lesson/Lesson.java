package ru.schoolsun.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.schoolsun.course.Course;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Getter
@Setter
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false, length = 190)
    private String title;

    @Column(nullable = false)
    private int position;

    @Column(name = "theory_md", columnDefinition = "TEXT")
    private String theoryMd;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
