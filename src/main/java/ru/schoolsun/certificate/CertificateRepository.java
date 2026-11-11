package ru.schoolsun.certificate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findBySerialNumber(String serialNumber);

    Optional<Certificate> findByCourseIdAndStudentId(Long courseId, Long studentId);

    List<Certificate> findByStudentIdOrderByIssuedAtDesc(Long studentId);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);
}
