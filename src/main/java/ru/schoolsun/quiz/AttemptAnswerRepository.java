package ru.schoolsun.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(Long attemptId);

    List<AttemptAnswer> findByAttemptIdIn(List<Long> attemptIds);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);
}
