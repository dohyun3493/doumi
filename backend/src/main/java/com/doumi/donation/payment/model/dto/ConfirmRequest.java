package com.doumi.donation.payment.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmRequest {
    @NotBlank
    private String paymentKey;

    @NotBlank
    private String orderId;

    @Min(1)
    private long amount;
}
