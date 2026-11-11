package ru.schoolsun.course;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import ru.schoolsun.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 190)
    private String title;

    @Column(nullable = false, unique = true, length = 190)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subject subject;

    @Column(name = "grade")
    private Integer grade;

    /** Ключ цветового акцента карточки: sun / sky / ember */
    @Column(nullable = false, length = 20)
    private String accent = "sun";

    @Column(name = "sequential_mode", nullable = false)
    private boolean sequentialMode;

    @Column(nullable = false)
    private boolean published;

    /** Спецкурс: не виден в каталоге, запись только по ссылке-приглашению */
    @Column(name = "invite_only", nullable = false)
    private boolean inviteOnly;

    /** Код ссылки-приглашения (/join/{code}) */
    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
