package ru.schoolsun.lesson;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderByPositionAsc(Long courseId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndPublishedTrue(Long courseId);
}
