package ru.schoolsun.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.schoolsun.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
