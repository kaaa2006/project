package org.team.mealkitshop.reports.repo;

import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.common.OrderStatus;
import org.team.mealkitshop.domain.order.QOrder;
import org.team.mealkitshop.reports.dto.ExecSummaryDTO;
import org.team.mealkitshop.reports.dto.SalesDailyPointDTO;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportsQueryRepository {

    private final JPAQueryFactory qf;

    /* 경영 요약: [from, to) */
    public ExecSummaryDTO findExecSummary(LocalDate from, LocalDate to) {
        QOrder o = QOrder.order;

        var predicate = o.orderDate.goe(from.atStartOfDay())
                .and(o.orderDate.lt(to.atStartOfDay()))
                .and(o.status.notIn(OrderStatus.CANCELED, OrderStatus.REFUNDED));

        // countDistinct()는 Long로 귀결되므로 그대로 사용
        Long orders = qf.select(o.orderId.countDistinct())
                .from(o)
                .where(predicate)
                .fetchOne();

        // sum()은 컬럼 타입(Integer/Long/BigDecimal 등)을 따르므로 Number로 받고 longValue() 사용
        Number grossNum = qf.select(o.productsTotal.sum())
                .from(o)
                .where(predicate)
                .fetchOne();

        Number discNum = qf.select(o.discountTotal.sum())
                .from(o)
                .where(predicate)
                .fetchOne();

        long ord = orders == null ? 0L : orders;
        long g   = grossNum == null ? 0L : grossNum.longValue();
        long ds  = discNum  == null ? 0L : discNum.longValue();
        long net = g - ds;

        return new ExecSummaryDTO(from, to, "ALL", ord, g, ds, net);
    }

    /* 일별 매출 시계열: [from, to) */
    public List<SalesDailyPointDTO> findSalesDaily(LocalDate from, LocalDate to) {
        QOrder o = QOrder.order;

        // "YYYY-MM-DD" 문자열로 그룹핑 (호환성 ↑)
        var dayStr = Expressions.stringTemplate("DATE_FORMAT({0}, {1})", o.orderDate, "%Y-%m-%d");

        var predicate = o.orderDate.goe(from.atStartOfDay())
                .and(o.orderDate.lt(to.atStartOfDay()))
                .and(o.status.notIn(OrderStatus.CANCELED, OrderStatus.REFUNDED));

        var exprOrders = o.orderId.countDistinct();
        var exprGross  = o.productsTotal.sum();
        var exprDisc   = o.discountTotal.sum();

        return qf.select(dayStr, exprOrders, exprGross, exprDisc)
                .from(o)
                .where(predicate)
                .groupBy(dayStr)
                .orderBy(dayStr.asc())
                .fetch()
                .stream()
                .map(t -> {
                    String ds = t.get(dayStr);
                    Long orders = t.get(exprOrders);
                    Number grossNum = t.get(exprGross);
                    Number discNum  = t.get(exprDisc);

                    long ord = orders == null ? 0L : orders;
                    long g   = grossNum == null ? 0L : grossNum.longValue();
                    long dsct= discNum  == null ? 0L : discNum.longValue();
                    long net = g - dsct;
                    long aov = ord == 0 ? 0L : (net / ord);

                    return new SalesDailyPointDTO(LocalDate.parse(ds), ord, net, aov);
                })
                .toList();
    }
}
