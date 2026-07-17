package com.doumi.donation.point.model.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UseRequest {
    @Min(1)
    private long campaignId;

    @Min(1)
    private long amount;
}
