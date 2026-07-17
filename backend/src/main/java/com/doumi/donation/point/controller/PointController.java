package com.doumi.donation.point.controller;

import com.doumi.donation.point.model.dto.ChargeRequest;
import com.doumi.donation.point.model.dto.UseRequest;
import com.doumi.donation.point.service.PointService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    public PointController(PointService pointService) {
        this.pointService = pointService;
    }

    @PostMapping("/charge")   
    public ResponseEntity<?> charge(@AuthenticationPrincipal Long memberId,
                                    @RequestBody @Valid ChargeRequest req) {
        pointService.charge(memberId, req);
        return ResponseEntity.ok("포인트 충전이 완료되었습니다.");
    }

    @PostMapping("/use")
    public ResponseEntity<?> use(@AuthenticationPrincipal Long memberId,
                                 @RequestBody @Valid UseRequest req) {
        pointService.use(memberId, req);
        return ResponseEntity.ok("포인트 사용이 완료되었습니다.");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(pointService.getHistory(memberId));
    }
}
