package ru.schoolsun.course;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.user.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CourseService {

    private static final String CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final SecureRandom random = new SecureRandom();

    public CourseService(CourseRepository courses, EnrollmentRepository enrollments) {
        this.courses = courses;
        this.enrollments = enrollments;
    }

    public List<Course> search(String q, Subject subject, Integer grade) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return courses.search(query, subject, grade);
    }

    public List<Course> topPublished(int limit) {
        return courses.topByEnrollments().stream().limit(limit).toList();
    }

    public List<Course> byTeacher(User teacher) {
        return courses.findByTeacherIdOrderByCreatedAtDesc(teacher.getId());
    }

    public Course bySlug(String slug) {
        return courses.findBySlug(slug).orElseThrow(() -> new NotFoundException("Курс не найден"));
    }

    public Course byId(Long id) {
        return courses.findById(id).orElseThrow(() -> new NotFoundException("Курс не найден"));
    }

    /** Курс доступен учителю-владельцу и админу даже в черновике. */
    public boolean canManage(Course course, User user) {
        return user != null && user.getRole() != null
                && (user.getRole() == ru.schoolsun.user.Role.ADMIN
                    || course.getTeacher() != null && course.getTeacher().getId().equals(user.getId()));
    }

    @Transactional
    public Course create(CourseForm form, User teacher) {
        Course c = new Course();
        apply(c, form);
        c.setTeacher(teacher);
        c.setCreatedAt(LocalDateTime.now());
        c.setSlug(uniqueSlug(form.getTitle()));
        return courses.save(c);
    }

    @Transactional
    public void update(Course course, CourseForm form) {
        apply(course, form);
        courses.save(course);
    }

    public Course byInviteCode(String code) {
        return courses.findByInviteCode(code).orElseThrow(() -> new NotFoundException("Ссылка-приглашение не найдена"));
    }

    @Transactional
    public void regenerateInviteCode(Course course) {
        course.setInviteCode(newInviteCode());
        courses.save(course);
    }

    private String newInviteCode() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private void apply(Course c, CourseForm form) {
        c.setTitle(form.getTitle().trim());
        c.setDescription(form.getDescription());
        c.setSubject(Subject.valueOf(form.getSubject()));
        c.setGrade(form.getGrade());
        c.setAccent(form.getAccent());
        c.setSequentialMode(form.isSequentialMode());
        c.setPublished(form.isPublished());
        c.setInviteOnly(form.isInviteOnly());
        if (form.isInviteOnly() && (c.getInviteCode() == null || c.getInviteCode().isBlank())) {
            c.setInviteCode(newInviteCode());
        }
    }

    private String uniqueSlug(String title) {
        String base = "course-" + title.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-zа-я0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "course";
        String slug = base;
        int i = 2;
        while (courses.findBySlug(slug).isPresent()) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    @Transactional
    public void enroll(User student, Course course) {
        if (!enrollments.existsByCourseIdAndStudentId(course.getId(), student.getId())) {
            Enrollment e = new Enrollment();
            e.setCourse(course);
            e.setStudent(student);
            e.setCreatedAt(LocalDateTime.now());
            enrollments.save(e);
        }
    }

    public boolean isEnrolled(Long courseId, Long studentId) {
        return enrollments.existsByCourseIdAndStudentId(courseId, studentId);
    }

    public List<Enrollment> enrollmentsOf(User student) {
        return enrollments.findByStudentIdOrderByCreatedAtDesc(student.getId());
    }

    public List<Enrollment> studentsOf(Course course) {
        return enrollments.findByCourseIdOrderByCreatedAtAsc(course.getId());
    }

    public long studentCount(Long courseId) {
        return enrollments.countByCourseId(courseId);
    }

    @Transactional
    public void delete(Course course) {
        courses.delete(course);
    }
}
