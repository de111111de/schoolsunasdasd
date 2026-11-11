package ru.schoolsun.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonAttachmentRepository extends JpaRepository<LessonAttachment, Long> {

    List<LessonAttachment> findByLessonId(Long lessonId);
}
