package com.doumi.donation.payment.controller;

import com.doumi.donation.payment.model.dto.CancelRequest;
import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 토스 결제 승인 + 포인트 충전
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@AuthenticationPrincipal Long memberId,
                                     @RequestBody @Valid ConfirmRequest req) {
        paymentService.confirm(memberId, req);
        return ResponseEntity.ok("결제 및 포인트 충전이 완료되었습니다.");
    }

    // 내 충전(결제) 내역 — 환불 가능 여부 판단용
    @GetMapping
    public ResponseEntity<?> myPayments(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(paymentService.findByMemberId(memberId));
    }

    // 결제 취소(환불) — 충전했던 포인트를 회수하고 토스 결제를 취소
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal Long memberId,
                                    @PathVariable long paymentId,
                                    @RequestBody(required = false) CancelRequest req) {
        String reason = (req != null) ? req.getReason() : null;
        paymentService.cancel(memberId, paymentId, reason);
        return ResponseEntity.ok("결제가 취소(환불)되었습니다.");
    }
}
