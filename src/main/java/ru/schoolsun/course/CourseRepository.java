package ru.schoolsun.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findBySlug(String slug);

    List<Course> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Course> findAllByOrderByCreatedAtDesc();

    List<Course> findByPublishedTrueOrderByCreatedAtDesc();

    long countByPublishedTrue();

    @Query("""
            select c from Course c
            where c.published = true
              and c.inviteOnly = false
              and (:q is null or lower(c.title) like lower(concat('%', :q, '%')))
              and (:subject is null or c.subject = :subject)
              and (:grade is null or c.grade = :grade)
            order by c.createdAt desc
            """)
    List<Course> search(@Param("q") String q,
                        @Param("subject") Subject subject,
                        @Param("grade") Integer grade);

    @Query("""
            select c from Course c
            where c.published = true
              and c.inviteOnly = false
            order by (select count(e) from Enrollment e where e.course = c) desc, c.createdAt desc
            """)
    List<Course> topByEnrollments();

    Optional<Course> findByInviteCode(String code);
}
