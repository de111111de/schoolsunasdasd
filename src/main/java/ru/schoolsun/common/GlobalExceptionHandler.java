package ru.schoolsun.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException e) {
        return new ModelAndView("error/404", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView noResource(NoResourceFoundException e) {
        return new ModelAndView("error/404", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView accessDenied(AccessDeniedException e) {
        return new ModelAndView("error/403", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooBig(MaxUploadSizeExceededException e, RedirectAttributes ra) {
        ra.addFlashAttribute("toastError", "Файл слишком большой — максимум 10 МБ");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e, Model model) {
        log.error("Необработанная ошибка", e);
        model.addAttribute("message", "Что-то пошло не так. Мы уже разбираемся.");
        return new ModelAndView("error/500", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
