package org.team.mealkitshop.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class SalesDailyPageResponse {
    private List<SalesDailyPointDTO> items;
    private int page;        // 1-based
    private int size;
    private long totalItems; // 전체 일수(선택 기간 포함 일수)
    private int totalPages;

    private long sumOrders;    // 기간 총 주문수
    private long sumNetSales;  // 기간 총 순매출
    private long aov;          // 기간 AOV = sumNetSales / sumOrders
}
