package com.doumi.donation.bookmark.controller;

import com.doumi.donation.bookmark.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    /** 내가 찜한 캠페인 목록 (캠페인 정보 포함) */
    @GetMapping
    public ResponseEntity<?> getMyBookmarks(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(bookmarkService.getMyBookmarks(memberId));
    }

    @PostMapping("/{campaignId}")
    public ResponseEntity<?> addBookmark(@AuthenticationPrincipal Long memberId,
                                         @PathVariable long campaignId) {
        bookmarkService.addBookmark(memberId, campaignId);
        return ResponseEntity.status(HttpStatus.CREATED).body("캠페인을 찜했습니다.");
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<?> removeBookmark(@AuthenticationPrincipal Long memberId,
                                            @PathVariable long campaignId) {
        bookmarkService.removeBookmark(memberId, campaignId);
        return ResponseEntity.ok("찜을 해제했습니다.");
    }
}
