package ru.schoolsun.lesson;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.Course;
import ru.schoolsun.course.CourseService;
import ru.schoolsun.homework.HomeworkSubmission;
import ru.schoolsun.homework.HomeworkSubmissionRepository;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Выдача файлов с проверкой прав: вложения уроков и сданные домашние задания. */
@Controller
public class FileController {

    private final LessonAttachmentRepository attachments;
    private final HomeworkSubmissionRepository submissions;
    private final CourseService courses;
    private final FileStorageService files;

    public FileController(LessonAttachmentRepository attachments, HomeworkSubmissionRepository submissions,
                          CourseService courses, FileStorageService files) {
        this.attachments = attachments;
        this.submissions = submissions;
        this.courses = courses;
        this.files = files;
    }

    @GetMapping("/files/attachment/{id}")
    public ResponseEntity<Resource> attachment(@PathVariable Long id, @ModelAttribute("me") User me) {
        LessonAttachment a = attachments.findById(id)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
        Course course = a.getLesson().getCourse();
        boolean allowed = me != null && (
                me.getRole() == Role.ADMIN
                        || courses.canManage(course, me)
                        || (me.getRole() == Role.STUDENT && courses.isEnrolled(course.getId(), me.getId())));
        if (!allowed) {
            throw new NotFoundException("Файл не найден");
        }
        return download(a.getStoredPath(), a.getOriginalName());
    }

    @GetMapping("/files/homework/{submissionId}")
    public ResponseEntity<Resource> homework(@PathVariable Long submissionId, @ModelAttribute("me") User me) {
        HomeworkSubmission s = submissions.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
        Course course = s.getHomework().getLesson().getCourse();
        boolean allowed = me != null && (
                me.getRole() == Role.ADMIN
                        || courses.canManage(course, me)
                        || s.getStudent().getId().equals(me.getId()));
        if (!allowed) {
            throw new NotFoundException("Файл не найден");
        }
        return download(s.getStoredPath(), s.getOriginalName());
    }

    private ResponseEntity<Resource> download(String storedPath, String originalName) {
        if (storedPath == null) {
            throw new NotFoundException("Файл не приложен");
        }
        Path path = files.resolve(storedPath);
        Resource resource = new FileSystemResource(path);
        String safeName = (originalName == null ? "file" : originalName)
                .replace("\"", "").replace("\r", "").replace("\n", "");
        String encoded = java.net.URLEncoder.encode(safeName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
