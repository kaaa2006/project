package org.team.mealkitshop.repository.item;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.common.ItemSortType;
import org.team.mealkitshop.common.OrderStatus;          // OrderStatus: common 패키지
import org.team.mealkitshop.domain.item.QItem;
import org.team.mealkitshop.domain.item.QItemImage;
import org.team.mealkitshop.domain.item.QReview;
import org.team.mealkitshop.domain.order.QOrder;         // 판매량 정렬용 Q타입
import org.team.mealkitshop.domain.order.QOrderItem;     // 판매량 정렬용 Q타입
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.ListItemDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.querydsl.jpa.JPAExpressions.select;

@Repository
public class ItemRepositoryCustomImpl implements ItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ItemRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    /* ==================== 관리자 목록 ==================== */
    @Override
    @Transactional(readOnly = true)
    public Page<ListItemDTO> getAdminItemPage(ItemSearchDTO dto, Pageable pageable) {
        QItem item = QItem.item;
        QItemImage rep = QItemImage.itemImage;
        QItemImage tmp = new QItemImage("tmpRep");
        QReview review = QReview.review;

        QOrder order = QOrder.order;
        QOrderItem oi = QOrderItem.orderItem;

        // STOP 포함 (관리자 전체 조회)
        BooleanExpression[] where = buildWhere(dto, null, true);

        // 대표 이미지(대표 Y중 최소 id)
        var repImgMinIdSubquery = select(tmp.id.min())
                .from(tmp)
                .where(tmp.item.eq(item), tmp.repimgYn.isTrue());

        // 실판매가 계산식
        NumberExpression<Integer> salePriceExpr = Expressions.numberTemplate(
                Integer.class, "FLOOR({0} * (100 - {1}) / 100.0)",
                item.originalPrice, item.discountRate
        );

        // ✅ 판매량(= 수량 합계) 서브쿼리 — SHIPPED/DELIVERED/COMPLETED만 포함
        NumberExpression<Integer> salesQtyExpr = Expressions.numberTemplate(
                Integer.class,
                "({0})",
                select(oi.quantity.sum().coalesce(0))
                        .from(oi)
                        .join(oi.order, order)
                        .where(
                                oi.item.eq(item),
                                order.status.in(
                                        OrderStatus.SHIPPED,
                                        OrderStatus.DELIVERED,
                                        OrderStatus.COMPLETED
                                )
                        )
        );

        // 정렬: DTO 우선, 없으면 pageable.sort 기반
        ItemSortType effSort = (dto != null && dto.getSortType() != null)
                ? dto.getSortType()
                : mapSortFrom(pageable.getSort());

        List<ListItemDTO> content = queryFactory
                .select(Projections.constructor(
                        ListItemDTO.class,
                        item.id,
                        item.itemNm,
                        item.itemDetail,
                        rep.imgUrl,
                        salePriceExpr,
                        item.originalPrice,
                        item.discountRate,
                        item.itemLike.coalesce(0L),
                        review.rating.avg().coalesce(0.0),
                        review.id.countDistinct().coalesce(0L),
                        item.itemViewCnt.coalesce(0L),
                        Expressions.constant(false), // 관리자페이지 liked=False
                        item.itemSellStatus,
                        item.regTime
                ))
                .from(item)
                .leftJoin(item.images, rep).on(rep.id.eq(repImgMinIdSubquery))
                .leftJoin(item.reviews, review)
                .where(where)
                .groupBy(
                        item.id, item.itemNm, item.itemDetail,
                        rep.imgUrl,
                        item.originalPrice, item.discountRate,
                        item.itemLike, item.itemViewCnt,
                        item.itemSellStatus, item.regTime
                )
                // SALES_DESC일 때만 판매량 정렬 사용
                .orderBy(
                        (effSort == ItemSortType.SALES_DESC)
                                ? new OrderSpecifier[]{ salesQtyExpr.desc(), item.id.desc() }
                                : getSortOrder(effSort, item, review, salePriceExpr)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(item.id.countDistinct())
                .from(item)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /* ==================== 사용자 목록 ==================== */
    @Override
    @Transactional(readOnly = true)
    public Page<ListItemDTO> getListItemPage(ItemSearchDTO dto, Pageable pageable) {
        QItem item = QItem.item;
        QItemImage rep = QItemImage.itemImage;
        QItemImage tmp = new QItemImage("tmpRep");
        QReview review = QReview.review;

        QOrder order = QOrder.order;
        QOrderItem oi = QOrderItem.orderItem;

        // 사용자 기본조건: SELL + SOLD_OUT
        List<BooleanExpression> base = List.of(
                item.itemSellStatus.in(ItemSellStatus.SELL, ItemSellStatus.SOLD_OUT)
        );
        BooleanExpression[] where = buildWhere(dto, base, false);

        // 대표 이미지
        var repImgMinIdSubquery = select(tmp.id.min())
                .from(tmp)
                .where(tmp.item.eq(item), tmp.repimgYn.isTrue());

        // 실판매가 계산식
        NumberExpression<Integer> salePriceExpr = Expressions.numberTemplate(
                Integer.class, "FLOOR({0} * (100 - {1}) / 100.0)",
                item.originalPrice, item.discountRate
        );

        // 가격 범위 (사용자 전용)
        if (dto != null && dto.getMinPrice() != null) {
            where = append(where, salePriceExpr.goe(dto.getMinPrice()));
        }
        if (dto != null && dto.getMaxPrice() != null) {
            where = append(where, salePriceExpr.loe(dto.getMaxPrice()));
        }

        // ✅ 판매량(= 수량 합계) 서브쿼리
        NumberExpression<Integer> salesQtyExpr = Expressions.numberTemplate(
                Integer.class,
                "({0})",
                select(oi.quantity.sum().coalesce(0))
                        .from(oi)
                        .join(oi.order, order)
                        .where(
                                oi.item.eq(item),
                                order.status.in(
                                        OrderStatus.SHIPPED,
                                        OrderStatus.DELIVERED,
                                        OrderStatus.COMPLETED
                                )
                        )
        );

        ItemSortType effSort = (dto != null && dto.getSortType() != null)
                ? dto.getSortType()
                : mapSortFrom(pageable.getSort());

        List<ListItemDTO> content = queryFactory
                .select(Projections.constructor(
                        ListItemDTO.class,
                        item.id,
                        item.itemNm,
                        item.itemDetail,
                        rep.imgUrl,
                        salePriceExpr.as("price"),
                        item.originalPrice,
                        item.discountRate,
                        item.itemLike.coalesce(0L),
                        review.rating.avg().coalesce(0.0),
                        review.id.countDistinct().coalesce(0L),
                        item.itemViewCnt.coalesce(0L),
                        Expressions.constant(false), // 로그인 유저 liked는 서비스에서 세팅
                        item.itemSellStatus,
                        item.regTime
                ))
                .from(item)
                .leftJoin(item.images, rep).on(rep.id.eq(repImgMinIdSubquery))
                .leftJoin(item.reviews, review)
                .where(where)
                .groupBy(
                        item.id, item.itemNm, item.itemDetail,
                        rep.imgUrl,
                        item.originalPrice, item.discountRate,
                        item.itemLike, item.itemViewCnt,
                        item.itemSellStatus, item.regTime
                )
                .orderBy(
                        (effSort == ItemSortType.SALES_DESC)
                                ? new OrderSpecifier[]{ salesQtyExpr.desc(), item.id.desc() }
                                : getSortOrder(effSort, item, review, salePriceExpr)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(item.id.countDistinct())
                .from(item)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /* ==================== 검색/정렬 Helper ==================== */

    /** 공통 where절 생성 */
    private BooleanExpression[] buildWhere(ItemSearchDTO dto, List<BooleanExpression> extraConditions, boolean admin) {
        QItem item = QItem.item;
        List<BooleanExpression> conditions = new ArrayList<>();
        if (extraConditions != null) conditions.addAll(extraConditions);

        // 등록일, 판매상태, 검색조건
        conditions.add(regDtsAfter(dto != null ? dto.getSearchDateType() : null));
        conditions.add(sellStatusEq(dto != null ? dto.getItemSellStatus() : null));
        conditions.add(searchByLike(dto != null ? dto.getSearchBy() : null,
                dto != null ? dto.getSearchQuery() : null));

        if (dto != null) {
            if (admin) {
                // 관리자: Category 단위 검색
                if (dto.getCategory() != null) {
                    conditions.add(item.category.eq(dto.getCategory()));
                }
            } else {
                // 사용자: FoodItem 우선, 없으면 Category fallback
                if (dto.getFoodItem() != null) {
                    conditions.add(item.foodItem.eq(dto.getFoodItem()));
                } else if (dto.getCategory() != null) {
                    conditions.add(item.category.eq(dto.getCategory()));
                }
            }
            // 키워드 (상품명 LIKE)
            if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
                conditions.add(item.itemNm.containsIgnoreCase(dto.getKeyword().trim()));
            }
        }

        // 특가상품(할인율 50% 이상) / 신메뉴(최근 7일) — NPE 가드 포함
        if (dto != null && Boolean.TRUE.equals(dto.getSpecialDeal())) {
            conditions.add(item.discountRate.goe(50));
        }
        if (dto != null && Boolean.TRUE.equals(dto.getNewItem())) {
            conditions.add(item.regTime.after(LocalDateTime.now().minusDays(7)));
        }

        return conditions.stream()
                .filter(Objects::nonNull)
                .toArray(BooleanExpression[]::new);
    }

    /** 등록일 필터 */
    private BooleanExpression regDtsAfter(String searchDateType) {
        if (searchDateType == null || searchDateType.isEmpty()) return null;
        LocalDateTime dt = LocalDateTime.now();
        switch (searchDateType) {
            case "1d" -> dt = dt.minusDays(1);
            case "1w" -> dt = dt.minusWeeks(1);
            case "1m" -> dt = dt.minusMonths(1);
            case "6m" -> dt = dt.minusMonths(6);
            default -> { return null; }
        }
        return QItem.item.regTime.after(dt);
    }

    /** 판매상태 필터 */
    private BooleanExpression sellStatusEq(ItemSellStatus status) {
        return status == null ? null : QItem.item.itemSellStatus.eq(status);
    }

    /** 검색 타입별 like */
    private BooleanExpression searchByLike(String searchBy, String searchQuery) {
        if (searchBy == null || searchQuery == null || searchQuery.isBlank()) return null;
        String term = searchQuery.trim();
        return switch (searchBy) {
            case "itemNm"     -> QItem.item.itemNm.containsIgnoreCase(term);
            case "createdBy"  -> QItem.item.createdBy.containsIgnoreCase(term);
            case "itemDetail" -> QItem.item.itemDetail.containsIgnoreCase(term);
            default           -> null;
        };
    }

    /** Pageable.sort → ItemSortType 매핑 */
    private ItemSortType mapSortFrom(Sort sort) {
        Sort.Order o = sort.stream().findFirst().orElse(null);
        if (o == null) return ItemSortType.NEW;

        String prop = o.getProperty();
        boolean asc = o.isAscending();

        return switch (prop) {
            case "price"        -> asc ? ItemSortType.PRICE_ASC : ItemSortType.PRICE_DESC;
            case "regTime"      -> ItemSortType.NEW;
            case "reviewCount"  -> ItemSortType.REVIEW_DESC;
            case "avgRating"    -> ItemSortType.RATING_DESC;
            case "itemViewCnt"  -> ItemSortType.POPULAR_VIEW;
            default             -> ItemSortType.NEW;
        };
    }

    /** 정렬 조건 (기본) — SALES_DESC는 외부에서 처리되지만 컴파일 안전을 위해 포함 */
    private OrderSpecifier<?>[] getSortOrder(ItemSortType sortType, QItem item, QReview review,
                                             NumberExpression<Integer> salePriceExpr) {
        ItemSortType sort = (sortType != null) ? sortType : ItemSortType.NEW;
        return switch (sort) {
            case POPULAR_VIEW -> new OrderSpecifier[]{ item.itemViewCnt.desc(), item.id.desc() };
            case PRICE_ASC    -> new OrderSpecifier[]{ salePriceExpr.asc(), item.id.desc() };
            case PRICE_DESC   -> new OrderSpecifier[]{ salePriceExpr.desc(), item.id.desc() };
            case RATING_DESC  -> new OrderSpecifier[]{ review.rating.avg().coalesce(0.0).desc(), item.id.desc() };
            case REVIEW_DESC  -> new OrderSpecifier[]{ review.id.countDistinct().coalesce(0L).desc(), item.id.desc() };
            case SALES_DESC   -> new OrderSpecifier[]{ item.regTime.desc(), item.id.desc() }; // 안전 fallback
            case NEW          -> new OrderSpecifier[]{ item.regTime.desc(), item.id.desc() };
        };
    }

    /** 조건 배열 추가 */
    private BooleanExpression[] append(BooleanExpression[] arr, BooleanExpression extra) {
        if (extra == null) return arr;
        BooleanExpression[] out = new BooleanExpression[arr.length + 1];
        System.arraycopy(arr, 0, out, 0, arr.length);
        out[arr.length] = extra;
        return out;
    }
}
