package com.doumi.donation.payment.service;

import com.doumi.donation.payment.model.dto.ConfirmRequest;
import com.doumi.donation.payment.model.dto.Payment;

import java.util.List;

public interface PaymentService {
    void confirm(long memberId, ConfirmRequest req);
    void cancel(long memberId, long paymentId, String reason);
    List<Payment> findByMemberId(long memberId);
}
