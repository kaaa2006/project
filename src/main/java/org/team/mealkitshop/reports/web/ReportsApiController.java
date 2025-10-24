package org.team.mealkitshop.reports.web;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.team.mealkitshop.reports.dto.ExecSummaryDTO;
import org.team.mealkitshop.reports.dto.SalesDailyPageResponse;
import org.team.mealkitshop.reports.dto.SalesDailyPointDTO;
import org.team.mealkitshop.reports.repo.ReportsQueryRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsApiController {

    private final ReportsQueryRepository repo;

    private LocalDate toInclusive(LocalDate to) {
        return to.plusDays(1); // 내부 쿼리는 [from, to) 이므로 외부에서는 끝 포함을 위해 +1일
    }

    @GetMapping("/exec")
    public ExecSummaryDTO exec(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repo.findExecSummary(from, toInclusive(to));
    }

    @GetMapping("/sales/daily")
    public List<SalesDailyPointDTO> salesDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return repo.findSalesDaily(from, toInclusive(to));
    }

    /** 📄 테이블/합계용: 날짜 전체(0 포함)를 서버에서 채워서 페이지네이션 반환 */
    @GetMapping("/sales/daily/page")
    public SalesDailyPageResponse salesDailyPage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "40") int size
    ) {
        LocalDate toEx = toInclusive(to);

        // 1) 데이터 있는 일자만 DB에서 가져온 뒤
        List<SalesDailyPointDTO> raw = repo.findSalesDaily(from, toEx);

        // 2) 날짜 전 범위를 0으로 채우고 raw로 덮어쓰기
        Map<LocalDate, SalesDailyPointDTO> map = raw.stream()
                .collect(Collectors.toMap(SalesDailyPointDTO::getDate, v -> v));

        List<SalesDailyPointDTO> filled = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            SalesDailyPointDTO v = map.getOrDefault(d, new SalesDailyPointDTO(d, 0L, 0L, 0L));
            // aov 보정(0으로 저장되어 있을 수 있음)
            long aov = (v.getOrders() == 0) ? 0L : (v.getNetSales() / v.getOrders());
            filled.add(new SalesDailyPointDTO(v.getDate(), v.getOrders(), v.getNetSales(), aov));
        }

        // 3) 합계(기간 전체 기준)
        long sumOrders = filled.stream().mapToLong(SalesDailyPointDTO::getOrders).sum();
        long sumNet    = filled.stream().mapToLong(SalesDailyPointDTO::getNetSales).sum();
        long aov       = (sumOrders == 0) ? 0 : (sumNet / sumOrders);

        // 4) 페이지네이션 (1-based)
        int totalItems = filled.size();
        int totalPages = (int)Math.ceil(totalItems / (double)size);
        int safePage   = Math.min(Math.max(page, 1), Math.max(totalPages,1));
        int startIdx   = (safePage - 1) * size;
        int endIdx     = Math.min(startIdx + size, totalItems);
        List<SalesDailyPointDTO> slice = filled.subList(startIdx, endIdx);

        return new SalesDailyPageResponse(slice, safePage, size, totalItems, totalPages, sumOrders, sumNet, aov);
    }
}
