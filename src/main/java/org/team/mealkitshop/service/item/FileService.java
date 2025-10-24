package org.team.mealkitshop.service.item;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
@Log4j2
public class FileService {

    private final String uploadPath;

    public FileService(@Value("${uploadPath}") String uploadPath) {
        this.uploadPath = uploadPath;
    }

    @PostConstruct
    void checkUploadDir() {
        if (uploadPath == null || uploadPath.isBlank()) {
            throw new IllegalStateException("uploadPath is not set (application.properties)");
        }
        try {
            Files.createDirectories(getUploadRoot());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadPath, e);
        }
    }

    /** 기본 루트에 저장 (기존 사용처 호환) */
    public String uploadFile(String originalFileName, byte[] fileData) throws IOException {
        return uploadFileInternal(getUploadRoot(), originalFileName, fileData);
    }

    /** 기본 루트에 저장 (InputStream) */
    public String uploadFile(String originalFileName, InputStream in) throws IOException {
        return uploadFileInternal(getUploadRoot(), originalFileName, in);
    }

    /** ★ 하위 폴더(subdir)에 저장 (InputStream) — detail/item 분리를 위해 추가 */
    public String uploadFileIn(String subdir, String originalFileName, InputStream in) throws IOException {
        Objects.requireNonNull(subdir, "subdir must not be null");
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(in, "input stream must not be null");

        Path base = getUploadRoot();
        Path dir = base.resolve(subdir).normalize();
        Files.createDirectories(dir);

        String saved = newSavedName(originalFileName);
        Path target = safeResolve(dir, saved);

        try (in) { Files.copy(in, target); }
        catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        log.info("Saved file: {}", target);
        return saved; // 파일명만 반환 (상위에서 subdir과 합쳐서 사용)
    }

    /** 저장된 '파일명 또는 하위경로 포함 파일명'으로 삭제 */
    public boolean deleteBySavedName(String savedFileName) throws IOException {
        return deleteFileInternal(getUploadRoot(), savedFileName);
    }

    /* ================= 내부 유틸 ================= */

    private Path getUploadRoot() {
        return Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    private static String extractExt(String originalFileName) {
        String safe = Paths.get(originalFileName).getFileName().toString();
        int dot = safe.lastIndexOf('.');
        return (dot >= 0 && dot < safe.length() - 1)
                ? safe.substring(dot).toLowerCase(Locale.ROOT)
                : "";
    }

    private static Path safeResolve(Path dir, String savedFileName) {
        Path target = dir.resolve(savedFileName).normalize();
        if (!target.startsWith(dir)) throw new SecurityException("Invalid path traversal: " + savedFileName);
        return target;
    }

    private static String newSavedName(String originalFileName) {
        return UUID.randomUUID() + extractExt(originalFileName);
    }

    private String uploadFileInternal(Path dir, String originalFileName, byte[] fileData) throws IOException {
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(fileData, "fileData must not be null");

        Files.createDirectories(dir);
        String saved = newSavedName(originalFileName);
        Path target = safeResolve(dir, saved);
        Files.write(target, fileData, CREATE_NEW);
        log.info("Saved file: {} ({} bytes)", target, fileData.length);
        return saved;
    }

    private String uploadFileInternal(Path dir, String originalFileName, InputStream in) throws IOException {
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        Objects.requireNonNull(in, "input stream must not be null");

        Files.createDirectories(dir);
        String saved = newSavedName(originalFileName);
        Path target = safeResolve(dir, saved);
        try (in) {
            Files.copy(in, target);
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        log.info("Saved file: {}", target);
        return saved;
    }

    private boolean deleteFileInternal(Path dir, String savedFileName) throws IOException {
        Objects.requireNonNull(savedFileName, "savedFileName must not be null");
        // ⬇️ 추가: 선행 슬래시 방어 (윈도우의 '\'도 방어)
        String safeName = savedFileName.replace('\\','/');
        if (safeName.startsWith("/")) safeName = safeName.substring(1);

        Path target = safeResolve(dir, safeName);
        boolean deleted = Files.deleteIfExists(target);
        if (deleted) log.info("Deleted file: {}", target);
        else log.info("File not found for delete: {}", target);
        return deleted;
    }
}
