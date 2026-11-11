package ru.schoolsun.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    List<LessonProgress> findByStudentId(Long studentId);

    List<LessonProgress> findByLessonId(Long lessonId);

    long countByStudentId(Long studentId);
}
