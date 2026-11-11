package ru.schoolsun.lesson;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "lesson_attachments")
@Getter
@Setter
public class LessonAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(name = "stored_path", nullable = false, length = 300)
    private String storedPath;

    @Column(name = "original_name", nullable = false, length = 300)
    private String originalName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;
}
