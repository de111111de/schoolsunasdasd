package ru.schoolsun.homework;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    Optional<Homework> findByLessonId(Long lessonId);
}
