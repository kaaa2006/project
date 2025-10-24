package org.team.mealkitshop.service.item;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.config.ItemDeletePolicyProperties;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.dto.item.ItemDTO;
import org.team.mealkitshop.dto.item.ItemFormDTO;
import org.team.mealkitshop.dto.item.ItemImgDTO;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.ListItemDTO;
import org.team.mealkitshop.repository.cart.CartItemRepository;
import org.team.mealkitshop.repository.item.ItemLikeRepository;
import org.team.mealkitshop.repository.item.ItemRepository;
import org.team.mealkitshop.repository.item.ReviewImageRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;
import org.team.mealkitshop.repository.order.OrderItemRepository;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * 아이템 도메인 서비스
 * - 생성/조회/수정/삭제 정책
 * - 대표/갤러리 이미지와 상세 이미지 사용 분리(표시/정책)
 * - 상세 본문 이미지 고아 파일 정리
 */
@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class ItemService {

    /* ===================== 의존성 ===================== */

    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ItemLikeRepository itemLikeRepository;
    private final OrderItemRepository orderItemRepository; // 주문 참조 여부 확인
    private final CartItemRepository cartItemRepository;
    private final ItemDeletePolicyProperties deletePolicy; // STOP 정책

    // 이미지 CRUD 전담 서비스
    private final ItemImgService itemImgService;
    private final FileService fileService;

    /** 허용 정렬 필드 화이트리스트 */
    private static final Set<String> ALLOWED_SORT = Set.of("regTime", "id");


    /* ===================== CREATE ===================== */

    /** 아이템 생성(기본 유효성 + 상세설명 sanitize + 판매상태 동기화) */
    public Long create(ItemFormDTO dto) {
        Objects.requireNonNull(dto, "dto must not be null");

        if (dto.getItemNm() == null || dto.getItemNm().isBlank())
            throw new IllegalArgumentException("상품명(itemNm)은 필수입니다.");
        if (dto.getOriginalPrice() == null || dto.getOriginalPrice() < 0)
            throw new IllegalArgumentException("정가는 0 이상이어야 합니다.");
        if (dto.getDiscountRate() == null || dto.getDiscountRate() < 0 || dto.getDiscountRate() > 95)
            throw new IllegalArgumentException("할인율은 0~95 사이여야 합니다.");
        if (dto.getStockNumber() == null || dto.getStockNumber() < 0)
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        if (dto.getFoodItem() == null)
            throw new IllegalArgumentException("음식 종류(FoodItem)는 필수입니다.");

        dto.setItemDetail(sanitizeDetail(dto.getItemDetail()));

        Item item = dto.createItem();
        item.syncSellStatusByStockIfNotStopped();
        return itemRepository.save(item).getId();
    }


    /* ===================== READ ===================== */

    /** 단건 조회(+대표/갤러리/상세/리뷰통계) — 갤러리는 detail=false만, 상세는 distinct */
    @Transactional(readOnly = true)
    public ItemDTO read(Long itemId) {
        Item item = itemRepository.findDetailBundleById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        ItemDTO dto = toDTO(item);

        // 갤러리: 상품 이미지(detail=false)만, 대표 먼저 정렬
        List<ItemImgDTO> images = new ArrayList<>(itemImgService.listProduct(itemId));
        images.sort((a, b) -> {
            int repCmp = Boolean.compare(Boolean.TRUE.equals(b.getRepimgYn()), Boolean.TRUE.equals(a.getRepimgYn()));
            if (repCmp != 0) return repCmp;
            return Long.compare(
                    a.getId() == null ? Long.MAX_VALUE : a.getId(),
                    b.getId() == null ? Long.MAX_VALUE : b.getId()
            );
        });
        dto.setItemImages(images);

        // 대표 URL(상품 이미지 중)
        images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getRepimgYn()))
                .findFirst()
                .ifPresent(rep -> dto.setRepImgUrl(rep.getImgUrl()));
        if (dto.getRepImgUrl() == null && !images.isEmpty()) {
            dto.setRepImgUrl(images.get(0).getImgUrl());
        }

        // 상세 이미지 URL: 중복 제거(distinct)
        List<String> longImages = itemImgService.listDetailUrls(itemId).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .distinct()
                .toList();
        dto.setLongImages(longImages);

        // 리뷰 통계
        fillReviewStats(dto, itemId);
        return dto;
    }

    /** 관리자 목록(간단 조건) — 리뷰 통계 제외 */
    @Transactional(readOnly = true)
    public Page<ListItemDTO> list(String keyword, String itemSellStatus, Pageable pageable) {
        Pageable safe = sanitize(pageable);

        ItemSearchDTO cond = new ItemSearchDTO();
        if (StringUtils.hasText(keyword)) cond.setKeyword(keyword.trim());
        if (StringUtils.hasText(itemSellStatus)) {
            try { cond.setItemSellStatus(ItemSellStatus.valueOf(itemSellStatus)); }
            catch (IllegalArgumentException ignore) {}
        }

        return itemRepository.getAdminItemPage(cond, safe);
    }

    /** 사용자/목록용 페이지 DTO — 대표 이미지/리뷰 통계 배치 설정(detail=false 기준 대표) */
    @Transactional(readOnly = true)
    public Page<ItemDTO> listWithStats(Pageable pageable) {
        Pageable safe = sanitize(pageable);
        Page<Item> page = itemRepository.findAll(safe);

        List<Item> content = page.getContent();
        if (content.isEmpty()) return page.map(this::toDTO);

        List<Long> ids = content.stream().map(Item::getId).toList();

        var avgMap = reviewRepository.findAvgRatingByItemIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ReviewRepository.ItemAvgRating::getItemId,
                        v -> v.getAvgRating() != null ? v.getAvgRating() : 0.0
                ));
        var cntMap = reviewRepository.findReviewCountByItemIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        ReviewRepository.ItemReviewCount::getItemId,
                        v -> v.getReviewCount() != null ? v.getReviewCount() : 0L
                ));

        Map<Long, ItemImgDTO> repMap = itemImgService.getRepresentatives(ids); // detail=false 기준

        return page.map(item -> {
            ItemDTO dto = toDTO(item);
            dto.setAvgRating(avgMap.getOrDefault(item.getId(), 0.0));
            dto.setReviewCount(cntMap.getOrDefault(item.getId(), 0L));
            ItemImgDTO rep = repMap.get(item.getId());
            if (rep != null) dto.setRepImgUrl(rep.getImgUrl());
            return dto;
        });
    }


    /* ===================== UPDATE ===================== */

    /**
     * 아이템 수정
     * - 기존/신규 HTML 확보 → sanitize
     * - 상세 이미지 고아 파일 정리(/images/detail/** 중 신규 본문에 없는 것 삭제)
     * - 엔티티 갱신
     * - 대표/갤러리 이미지는 ItemImgService가 별도 관리
     */
    public ItemDTO update(Long id, ItemFormDTO dto) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));

        String oldHtml = item.getItemDetail();
        String newHtml = sanitizeDetail(dto.getItemDetail());
        dto.setItemDetail(newHtml);

        // 상세 이미지 고아 파일 정리
        for (String orphan : computeOrphanDetails(oldHtml, newHtml)) {
            try { fileService.deleteBySavedName(orphan); } catch (Exception ignore) {}
        }

        item.updateItem(dto);
        return toDTO(item);
    }


    /* ===================== DELETE ===================== */

    /**
     * 삭제 정책:
     *  - STOP이 아니면 STOP으로 전환 → "soft"
     *  - STOP이면 하드삭제 → "hard"
     *  - 그 외 → "blocked"
     */


    public String deleteWithPolicy(Long id, boolean forceStopOnly) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));

        if (forceStopOnly) {
            if (item.getItemSellStatus() != ItemSellStatus.STOP) {
                item.setItemSellStatus(ItemSellStatus.STOP);
                return "soft";
            }
            hardDeleteInternal(item);
            return "hard-forced";
        }

        // ===== 기존 정책 흐름 유지 =====
        ItemSellStatus cur = item.getItemSellStatus();
        if (cur != ItemSellStatus.STOP) {
            item.setItemSellStatus(ItemSellStatus.STOP);
            return "soft";
        }

        int retentionDays = Math.max(0, deletePolicy.retentionDays());
        var lastUpdated = item.getUpdateTime();
        boolean retentionPassed = (lastUpdated != null)
                && java.time.Duration.between(lastUpdated, java.time.LocalDateTime.now()).toDays() >= retentionDays;

        if (!retentionPassed) return "blocked";
        if (orderItemRepository.existsByItem_Id(id)) return "blocked";

        hardDeleteInternal(item);
        return "hard";
    }

    /** 하드 삭제 내부 — 모든 이미지/좋아요/리뷰 제거 후 본체 삭제 */
    private void hardDeleteInternal(Item item) {
        Long id = item.getId();

        //  먼저 주문 라인 삭제 (FK 충돌 방지)
        orderItemRepository.deleteByItemId(id);

        // 상품 이미지 삭제 (파일 + DB)
        var itemImgs = itemImgService.list(id); // DTO 리스트 반환
        for (ItemImgDTO dto : itemImgs) {
            try {
                if (dto.getImgName() != null) {
                    fileService.deleteBySavedName("item/" + dto.getImgName()); // 저장 파일 삭제
                }
            } catch (Exception e) {
                log.warn("Failed to delete item image file: {}", dto.getImgName(), e);
            }
        }
        if (!itemImgs.isEmpty()) {
            List<Long> imgIds = itemImgs.stream()
                    .map(ItemImgDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();
            itemImgService.deleteAll(id, imgIds);
        }

        //  좋아요/장바구니 정리
        itemLikeRepository.deleteByItem_Id(id);
        cartItemRepository.deleteByItemId(id);

        //  리뷰 이미지 삭제 (파일 + DB)
        var reviewImgs = reviewImageRepository.findByReview_Item_Id(id);
        for (var ri : reviewImgs) {
            try {
                if (ri.getImgName() != null) {
                    fileService.deleteBySavedName("review/" + ri.getImgName()); // 저장 파일 삭제
                }
            } catch (Exception e) {
                log.warn("Failed to delete review image file: {}", ri.getImgName(), e);
            }
        }
        if (!reviewImgs.isEmpty()) {
            reviewImageRepository.deleteByItemIdBulk(id);
        }

        // 리뷰 삭제
        reviewRepository.deleteByItemId(id);

        // 아이템 본체 삭제
        itemRepository.delete(item);
    }

    /** STOP → 재개(기본 로직) */
    public void restore(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
        item.setItemSellStatus(item.getStockNumber() > 0 ? ItemSellStatus.SELL : ItemSellStatus.SOLD_OUT);
    }


    /* ===================== CUSTOM 조회 ===================== */

    /** 관리자 목록 페이지 전용 조회(정렬/페이지 sanitize 적용) */
    @Transactional(readOnly = true)
    public Page<ListItemDTO> getAdminPage(ItemSearchDTO cond, Pageable pageable) {
        Pageable safe = sanitize(pageable);
        return itemRepository.getAdminItemPage(cond, safe);
    }

    /** 사용자 목록 페이지 전용 조회(컨디션/페이지 그대로 위임) */
    @Transactional(readOnly = true)
    public Page<ListItemDTO> getListPage(ItemSearchDTO cond, Pageable pageable) {
        return itemRepository.getListItemPage(cond, pageable);
    }

    /* ===================== 내부 유틸 ===================== */

    /** 정렬/페이지 파라미터 화이트리스트 적용 */
    private Pageable sanitize(Pageable p) {
        Sort s = Sort.by(
                StreamSupport.stream(p.getSort().spliterator(), false)
                        .filter(o -> ALLOWED_SORT.contains(o.getProperty()))
                        .map(o -> new Sort.Order(o.getDirection(), o.getProperty()))
                        .toList()
        );
        if (s.isUnsorted()) s = Sort.by(Sort.Order.desc("id"));
        int size = Math.min(p.getPageSize(), 50);
        return PageRequest.of(p.getPageNumber(), size, s);
    }

    /** 상세설명 HTML sanitize — 필요 시 허용 목록 확장 */
    private String sanitizeDetail(String html) {
        if (html == null) return null;
        if (html.isBlank()) return "";
        return html;
    }

    /**
     * HTML 본문에서 /images/detail/** 경로만 추출해
     * fileService.deleteBySavedName()에 사용할 저장 경로("detail/UUID.ext")로 반환.
     * - 절대/상대 URL 모두 대응, data: 무시
     */
    private static Set<String> extractDetailSavedNames(String html) {
        Set<String> out = new HashSet<>();
        if (html == null || html.isBlank()) return out;

        // baseUri="/" 로 파싱 — 상대경로 유지
        Document doc = Jsoup.parseBodyFragment(html, "/");
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src").trim();
            if (src.isEmpty() || src.startsWith("data:")) continue;

            // "http(s)://.../images/..." 또는 "/images/..." 모두 처리
            int pivot = src.indexOf("/images/");
            if (pivot < 0) continue;

            String tail = src.substring(pivot + "/images/".length()); // ex) "detail/UUID.jpg"
            // 쿼리/해시 제거
            int q = tail.indexOf('?'); if (q >= 0) tail = tail.substring(0, q);
            int h = tail.indexOf('#'); if (h >= 0) tail = tail.substring(0, h);

            if (tail.startsWith("detail/")) out.add(tail);
        }
        return out;
    }

    /** 기존/신규 본문 비교로 고아 상세 이미지 계산 */
    private Set<String> computeOrphanDetails(String oldHtml, String newHtml) {
        try {
            Set<String> oldSet = extractDetailSavedNames(oldHtml);
            Set<String> newSet = extractDetailSavedNames(newHtml);
            oldSet.removeAll(newSet);
            return oldSet;
        } catch (Exception e) {
            return java.util.Collections.emptySet();
        }
    }

    /** DTO에 리뷰 통계 세팅 */
    private void fillReviewStats(ItemDTO dto, Long itemId) {
        Double avg = reviewRepository.getAverageRatingByItemId(itemId);
        long cnt = reviewRepository.countByItem_Id(itemId);
        dto.setAvgRating(avg != null ? avg : 0.0);
        dto.setReviewCount(cnt);
    }

    /** 엔티티 → DTO 기본 매핑(대표/상세 URL은 후처리) */
    private ItemDTO toDTO(Item i) {
        ItemDTO dto = new ItemDTO();
        dto.setId(i.getId());
        dto.setItemNm(i.getItemNm());
        dto.setPrice(i.getSalePrice());
        dto.setOriginalPrice(i.getOriginalPrice());
        dto.setDiscountRate(i.getDiscountRate());
        dto.setStockNumber(i.getStockNumber());
        dto.setItemDetail(i.getItemDetail());
        dto.setItemSellStatus(i.getItemSellStatus());
        dto.setCategory(i.getCategory());
        dto.setFoodItem(i.getFoodItem());
        dto.setItemLike(i.getItemLike());
        dto.setItemViewCnt(i.getItemViewCnt());
        dto.setRegTime(i.getRegTime());
        dto.setUpdateTime(i.getUpdateTime());
        dto.setCreatedBy(i.getCreatedBy());
        dto.setModifiedBy(i.getModifiedBy());
        // 대표 이미지 URL(상품용만) — read()/listWithStats에서 보강
        dto.setRepImgUrl(
                itemImgService.getRepresentative(i.getId())
                        .map(ItemImgDTO::getImgUrl)
                        .orElse(null)
        );
        // 상세 이미지 URL 목록 — read()에서 distinct로 보강
        dto.setLongImages(itemImgService.listDetailUrls(i.getId()));
        return dto;
    }
}
