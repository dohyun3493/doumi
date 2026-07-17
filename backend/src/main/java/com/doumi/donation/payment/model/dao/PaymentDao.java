package com.doumi.donation.payment.model.dao;

import com.doumi.donation.payment.model.dto.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentDao {
    void insertPayment(Payment payment);
    List<Payment> findByMemberId(@Param("memberId") long memberId);
    boolean existsByPaymentKey(@Param("paymentKey") String paymentKey);
    Payment findById(@Param("paymentId") long paymentId);
    /** CHARGED → CANCELED 전이. 이미 취소된 건은 갱신되지 않음(0 반환) */
    int cancelPayment(@Param("paymentId") long paymentId);
}
