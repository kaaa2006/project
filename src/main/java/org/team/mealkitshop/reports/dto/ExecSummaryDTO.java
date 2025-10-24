package org.team.mealkitshop.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ExecSummaryDTO {
    private LocalDate from;
    private LocalDate to;
    private String channel; // 현재 채널 미구현 → "ALL" 고정 용도
    private long orders;
    private long grossSales;   // Σ productsTotal
    private long discounts;    // Σ discountTotal
    private long netSales;     // Σ (productsTotal - discountTotal)  [배송비/환불 제외]
}
