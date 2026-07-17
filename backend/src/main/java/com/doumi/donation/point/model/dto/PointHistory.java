package com.doumi.donation.point.model.dto;

import lombok.Data;

@Data
public class PointHistory {
    private long historyId;
    private long memberId;
    private String type; // CHARGE, USE
    private long amount;
    private String createdAt;
}
