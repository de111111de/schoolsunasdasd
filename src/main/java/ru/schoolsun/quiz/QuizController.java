package ru.schoolsun.quiz;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ru.schoolsun.common.NotFoundException;
import ru.schoolsun.course.CourseService;
import ru.schoolsun.lesson.Lesson;
import ru.schoolsun.lesson.LessonService;
import ru.schoolsun.user.Role;
import ru.schoolsun.user.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/student/quiz")
public class QuizController {

    private final QuizService quizService;
    private final CourseService courses;
    private final LessonService lessonService;

    public QuizController(QuizService quizService, CourseService courses, LessonService lessonService) {
        this.quizService = quizService;
        this.courses = courses;
        this.lessonService = lessonService;
    }

    public record QRow(Question question, List<Option> options, List<String> pairs) {}

    private void guard(Quiz quiz, User me) {
        Lesson lesson = quiz.getLesson();
        if (!lesson.isPublished() || !lesson.getCourse().isPublished()) {
            throw new NotFoundException("Тест недоступен");
        }
        if (me.getRole() != Role.STUDENT || !courses.isEnrolled(lesson.getCourse().getId(), me.getId())) {
            throw new NotFoundException("Сначала запишись на курс");
        }
    }

    @GetMapping("/{quizId}")
    public String take(@PathVariable Long quizId, @ModelAttribute("me") User me,
                       Model model, RedirectAttributes ra) {
        Quiz quiz = quizService.byId(quizId);
        guard(quiz, me);
        QuizAttempt attempt;
        try {
            attempt = quizService.startOrResume(me, quiz);
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
            return "redirect:/student/lessons/" + quiz.getLesson().getId();
        }

        List<QRow> rows = new ArrayList<>();
        for (Question q : quizService.questionsOf(quiz.getId())) {
            List<Option> opts = quizService.optionsOf(q.getId());
            Set<String> pairs = new LinkedHashSet<>();
            if (q.getType() == QuestionType.MATCH) {
                opts.forEach(o -> pairs.add(o.getPairKey()));
            }
            rows.add(new QRow(q, opts, new ArrayList<>(pairs)));
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempt", attempt);
        model.addAttribute("rows", rows);
        model.addAttribute("attemptNo", attempt.getAttemptNo());
        model.addAttribute("maxAttempts", quiz.getMaxAttempts());
        model.addAttribute("remaining", quizService.remainingSeconds(attempt));
        return "quiz/take";
    }

    @PostMapping("/{quizId}")
    public String submit(@PathVariable Long quizId, HttpServletRequest request,
                         @ModelAttribute("me") User me, RedirectAttributes ra) {
        Quiz quiz = quizService.byId(quizId);
        guard(quiz, me);
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, String.join(",", v)));
        try {
            QuizAttempt attempt = quizService.submit(me, quiz, params);
            return "redirect:/student/quiz/attempt/" + attempt.getId();
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("toastError", e.getMessage());
            return "redirect:/student/lessons/" + quiz.getLesson().getId();
        }
    }

    @GetMapping("/attempt/{attemptId}")
    public String result(@PathVariable Long attemptId, @ModelAttribute("me") User me,
                         Model model, RedirectAttributes ra) {
        QuizAttempt attempt = quizService.attemptById(attemptId);
        if (!attempt.getStudent().getId().equals(me.getId())) {
            throw new NotFoundException("Это не твоя попытка");
        }
        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", attempt.getQuiz());
        model.addAttribute("lesson", attempt.getQuiz().getLesson());
        model.addAttribute("percent", quizService.percent(attempt));
        model.addAttribute("passed", quizService.passed(attempt));
        model.addAttribute("rows", quizService.resultRows(attempt));
        model.addAttribute("pending", attempt.getStatus() == AttemptStatus.PENDING_REVIEW);
        return "quiz/result";
    }
}
