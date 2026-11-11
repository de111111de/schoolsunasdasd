package ru.schoolsun.homework;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.user.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HomeworkService {

    private final HomeworkRepository homeworks;
    private final HomeworkSubmissionRepository submissions;
    private final ru.schoolsun.lesson.FileStorageService files;
    private final LessonService lessons;

    public HomeworkService(HomeworkRepository homeworks, HomeworkSubmissionRepository submissions,
                           ru.schoolsun.lesson.FileStorageService files, LessonService lessons) {
        this.homeworks = homeworks;
        this.submissions = submissions;
        this.files = files;
        this.lessons = lessons;
    }

    public Homework byId(Long id) {
        return homeworks.findById(id).orElseThrow(() -> new NotFoundException("Домашнее задание не найдено"));
    }

    @Transactional
    public Homework save(Lesson lesson, HomeworkForm form) {
        Homework hw = homeworks.findByLessonId(lesson.getId()).orElseGet(() -> {
            Homework h = new Homework();
            h.setLesson(lesson);
            return h;
        });
        hw.setTitle(form.getTitle().trim());
        hw.setInstruction(form.getInstruction());
        hw.setMaxPoints(form.getMaxPoints() == null || form.getMaxPoints() < 1 ? 10 : form.getMaxPoints());
        hw.setDeadline(form.getDeadline());
        return homeworks.save(hw);
    }

    public HomeworkSubmission submissionOf(Long homeworkId, Long studentId) {
        return submissions.findByHomeworkIdAndStudentId(homeworkId, studentId).orElse(null);
    }

    @Transactional
    public void submit(User student, Homework hw, String textAnswer, MultipartFile file) {
        if ((textAnswer == null || textAnswer.isBlank()) && (file == null || file.isEmpty())) {
            throw new IllegalArgumentException("Приложи файл или напиши ответ");
        }
        HomeworkSubmission s = submissions.findByHomeworkIdAndStudentId(hw.getId(), student.getId())
                .orElseGet(HomeworkSubmission::new);
        if (s.getId() != null && s.getStatus() == SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Работа уже сдана — дождись проверки учителя");
        }
        if (s.getId() != null && s.getStatus() == SubmissionStatus.ACCEPTED) {
            throw new IllegalStateException("Работа уже принята");
        }
        if (file != null && !file.isEmpty()) {
            if (s.getStoredPath() != null) {
                files.delete(s.getStoredPath());
            }
            s.setStoredPath(files.store(file));
            s.setOriginalName(file.getOriginalFilename());
        }
        s.setHomework(hw);
        s.setStudent(student);
        s.setTextAnswer(textAnswer);
        s.setStatus(SubmissionStatus.SUBMITTED);
        s.setSubmittedAt(LocalDateTime.now());
        s.setScore(null);
        s.setFeedback(null);
        submissions.save(s);
    }

    public List<HomeworkSubmission> submissionsOfTeacher(Long teacherId) {
        return submissions.findByHomeworkLessonCourseTeacherIdOrderBySubmittedAtDesc(teacherId);
    }

    public List<HomeworkSubmission> submissionsOfCourse(Long courseId) {
        return submissions.findByHomeworkLessonCourseIdOrderBySubmittedAtDesc(courseId);
    }

    @Transactional
    public void grade(Long submissionId, int score, SubmissionStatus status, String feedback, User teacher) {
        HomeworkSubmission s = submissions.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Сдача не найдена"));
        User owner = s.getHomework().getLesson().getCourse().getTeacher();
        if (!owner.getId().equals(teacher.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Это не твой курс");
        }
        int max = s.getHomework().getMaxPoints();
        s.setScore(Math.max(0, Math.min(score, max)));
        s.setStatus(status);
        s.setFeedback(feedback);
        s.setGradedAt(LocalDateTime.now());
        s.setGradedBy(teacher);
        submissions.save(s);
        if (status == SubmissionStatus.ACCEPTED) {
            lessons.completeIfEligible(s.getHomework().getLesson(), s.getStudent());
        }
    }
}
