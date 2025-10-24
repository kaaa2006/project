package org.team.mealkitshop.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SalesDailyPointDTO {
    private LocalDate date;   // 일자
    private long orders;      // 건수
    private long netSales;    // 순매출(배송비/환불 제외)
    private long aov;         // netSales / orders
}
