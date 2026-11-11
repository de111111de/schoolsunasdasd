package ru.schoolsun.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findFirstByQuizIdAndStudentIdOrderByAttemptNoDesc(Long quizId, Long studentId);

    List<QuizAttempt> findByQuizIdAndStudentIdOrderByAttemptNoDesc(Long quizId, Long studentId);

    List<QuizAttempt> findByQuizId(Long quizId);

    List<QuizAttempt> findByStatus(AttemptStatus status);

    List<QuizAttempt> findByQuizLessonCourseTeacherIdAndStatus(Long teacherId, AttemptStatus status);
}
