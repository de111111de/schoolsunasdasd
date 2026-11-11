package ru.schoolsun.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    Optional<Enrollment> findByCourseIdAndStudentId(Long courseId, Long studentId);

    List<Enrollment> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Enrollment> findByCourseIdOrderByCreatedAtAsc(Long courseId);

    long countByCourseId(Long courseId);
}
