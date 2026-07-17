package com.doumi.donation.point.model.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ChargeRequest {
    @Min(1)
    private long amount;
}
