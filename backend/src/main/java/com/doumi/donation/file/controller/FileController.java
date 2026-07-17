package com.doumi.donation.file.controller;

import com.doumi.donation.file.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 공용 이미지 업로드 엔드포인트.
 * 클라이언트가 이미지를 먼저 업로드해 URL을 받은 뒤, 그 URL을 각 도메인 API(캠페인/프로필/보고서) JSON 에 담아 보낸다.
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** 이미지 1장을 업로드하고 접근 URL을 반환한다. (로그인 필요) */
    @PostMapping
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("url", url));
    }
}
