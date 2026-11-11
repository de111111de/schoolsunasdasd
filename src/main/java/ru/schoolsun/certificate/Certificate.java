package ru.schoolsun.certificate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private ru.schoolsun.course.Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private ru.schoolsun.user.User student;

    @Column(name = "serial_number", nullable = false, unique = true, length = 40)
    private String serialNumber;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
}
