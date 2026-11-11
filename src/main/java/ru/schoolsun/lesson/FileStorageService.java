package ru.schoolsun.lesson;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ru.schoolsun.common.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/** Хранение загруженных файлов в локальной папке (настраивается app.upload.dir). */
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "png", "jpg", "jpeg", "gif", "txt", "doc", "docx", "xls", "xlsx",
            "ppt", "pptx", "zip", "rar", "csv", "py", "java", "md");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Path.of(uploadDir);
    }

    /** Сохраняет файл с случайным безопасным именем, возвращает относительный путь. */
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Тип файла ." + ext + " не поддерживается");
        }
        try {
            Files.createDirectories(root);
            String stored = UUID.randomUUID() + "." + ext;
            Path target = root.resolve(stored).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Некорректный путь файла");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return stored;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл", e);
        }
    }

    public Path resolve(String storedName) {
        Path p = root.resolve(storedName).normalize();
        if (!p.startsWith(root)) {
            throw new NotFoundException("Файл не найден");
        }
        return p;
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException e) {
            // файл мог уже исчезнуть — это не ошибка запроса
        }
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }
}
