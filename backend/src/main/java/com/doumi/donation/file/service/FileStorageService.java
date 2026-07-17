package com.doumi.donation.file.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 업로드 파일을 서버 로컬 디스크에 저장하고, 정적 서빙용 URL(/uploads/...)을 돌려준다.
 * 저장 위치는 application.properties 의 app.upload.dir 로 지정한다.
 */
@Service
public class FileStorageService {

    /** 허용 이미지 확장자 (소문자) */
    private static final List<String> ALLOWED_EXT = List.of("jpg", "jpeg", "png", "gif", "webp");

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 서버 기동 시 업로드 디렉터리가 없으면 생성한다. */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadRoot, e);
        }
    }

    /**
     * 이미지 파일을 저장하고 접근 URL(/uploads/{파일명})을 반환한다.
     *
     * @param file 업로드된 멀티파트 파일 (이미지)
     * @return 정적 서빙 경로 (예: /uploads/3f1c....png)
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = StringUtils.getFilenameExtension(original);
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("이미지 파일(jpg, jpeg, png, gif, webp)만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 형식의 파일만 업로드할 수 있습니다.");
        }

        // UUID 기반의 충돌 없는 파일명 생성 (확장자는 소문자로 통일)
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + "." + ext.toLowerCase(Locale.ROOT);

        try {
            Path target = uploadRoot.resolve(storedName).normalize();
            // 경로 탈출(path traversal) 방어
            if (!target.getParent().equals(uploadRoot)) {
                throw new IllegalArgumentException("잘못된 파일 경로입니다.");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }

        return "/uploads/" + storedName;
    }

    /** 저장된 이미지를 (mime, bytes)로 다시 읽는다. AI 멀티모달 입력용. */
    public StoredImage load(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            throw new IllegalArgumentException("잘못된 이미지 경로입니다: " + url);
        }
        String name = url.substring("/uploads/".length());
        Path target = uploadRoot.resolve(name).normalize();
        if (!target.getParent().equals(uploadRoot)) {
            throw new IllegalArgumentException("잘못된 이미지 경로입니다.");
        }
        try {
            byte[] bytes = Files.readAllBytes(target);
            String ext = StringUtils.getFilenameExtension(name);
            String mime = "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext) ? "image/jpeg"
                    : "png".equalsIgnoreCase(ext) ? "image/png"
                    : "gif".equalsIgnoreCase(ext) ? "image/gif"
                    : "webp".equalsIgnoreCase(ext) ? "image/webp"
                    : "application/octet-stream";
            return new StoredImage(mime, bytes);
        } catch (IOException e) {
            throw new IllegalStateException("이미지를 읽을 수 없습니다: " + url, e);
        }
    }

    /** 디스크에서 읽은 이미지 (AI 입력용) */
    public record StoredImage(String mimeType, byte[] bytes) {}
}
