package ru.schoolsun.homework;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {

    Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    List<HomeworkSubmission> findByHomeworkLessonCourseTeacherIdOrderBySubmittedAtDesc(Long teacherId);

    List<HomeworkSubmission> findByHomeworkLessonCourseIdOrderBySubmittedAtDesc(Long courseId);

    List<HomeworkSubmission> findByStudentId(Long studentId);
}
