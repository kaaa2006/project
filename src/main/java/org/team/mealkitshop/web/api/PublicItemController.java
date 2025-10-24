package org.team.mealkitshop.web.api; // ← 패키지 경로 맞게 수정

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

// ⛳️ ⬇⬇⬇ 여기를 당신 프로젝트의 실제 패키지로 변경 ⬇⬇⬇
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemImage;
// ⛳️ ⬆⬆⬆ Item / ItemImage 의 실제 패키지로 교체 ⬆⬆⬆

@RestController
@RequestMapping("/api/items")
public class PublicItemController {

    @PersistenceContext
    private EntityManager em;

    // ============ 목록 ============
    @GetMapping
    @Transactional
    public ResponseEntity<PageResponse<MainItemCard>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "regTime,DESC") String sort,
            @RequestParam(required = false) String keyword
    ) {
        // 정렬 파싱 (허용된 컬럼만)
        String[] sd = sort.split(",", 2);
        String sortKey = (sd.length > 0 && !sd[0].isBlank()) ? sd[0].trim() : "regTime";
        String sortDir = (sd.length > 1 && "ASC".equalsIgnoreCase(sd[1].trim())) ? "ASC" : "DESC";
        Set<String> allow = Set.of("regTime", "price", "itemNm", "id");
        if (!allow.contains(sortKey)) sortKey = "regTime";

        // where절
        String where = " where 1=1";
        if (keyword != null && !keyword.isBlank()) where += " and lower(i.itemNm) like :kw";

        String base = " from Item i" + where;
        String orderBy = " order by i." + sortKey + " " + sortDir;

        // 총건수
        TypedQuery<Long> cq = em.createQuery("select count(i)" + base, Long.class);
        if (keyword != null && !keyword.isBlank()) cq.setParameter("kw", "%" + keyword.toLowerCase() + "%");
        long total = cq.getSingleResult();

        // 목록
        TypedQuery<Item> q = em.createQuery("select i" + base + orderBy, Item.class)
                .setFirstResult(page * size)
                .setMaxResults(size);
        if (keyword != null && !keyword.isBlank()) q.setParameter("kw", "%" + keyword.toLowerCase() + "%");
        List<Item> items = q.getResultList();

        // 대표이미지 (itemId -> url) 한번에
        List<Long> ids = items.stream().map(Item::getId).filter(Objects::nonNull).toList();
        Map<Long, String> repUrlMap = ids.isEmpty() ? Map.of() : loadRepImageMap(ids);

        // 카드 DTO 매핑 (instanceof 없이 안전 변환)
        List<MainItemCard> content = new ArrayList<>(items.size());
        for (Item it : items) {
            String category = (it.getCategory() != null) ? it.getCategory().name() : null;
            int price = toInt(it.getSalePrice());
            int like = toInt(it.getItemLike());
            int view = toInt(it.getItemViewCnt());
            content.add(new MainItemCard(
                    it.getId(),
                    it.getItemNm(),
                    repUrlMap.getOrDefault(it.getId(), ""),
                    price,
                    like,
                    view,
                    category,
                    it.getRegTime()
            ));
        }

        int totalPages = (int) Math.max(1, Math.ceil(total / (double) size));
        PageResponse<MainItemCard> body = new PageResponse<>(content, total, totalPages, page, size);
        return ResponseEntity.ok(body);
    }

    // ============ 상세 ============
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<ItemDetailView> detail(@PathVariable Long id) {
        Item item = em.find(Item.class, id);
        if (item == null) return ResponseEntity.notFound().build();

        // 이미지 전체 (대표 먼저)
        TypedQuery<ItemImage> iq = em.createQuery(
                "select ii from ItemImage ii where ii.item.id = :id order by ii.repimgYn desc, ii.id asc",
                ItemImage.class
        );
        List<ItemImage> images = iq.setParameter("id", id).getResultList();

        String repUrl = images.stream()
                .filter(ii -> Boolean.TRUE.equals(ii.getRepimgYn()))
                .map(ItemImage::getImgUrl)
                .findFirst()
                .orElseGet(() -> images.stream().findFirst().map(ItemImage::getImgUrl).orElse(null));

        List<ImgVM> thumbs = new ArrayList<>(images.size());
        List<String> longImages = new ArrayList<>();
        for (ItemImage ii : images) {
            boolean isRep = Boolean.TRUE.equals(ii.getRepimgYn());
            thumbs.add(new ImgVM(ii.getImgUrl(), isRep));
            if (!isRep) longImages.add(ii.getImgUrl());
        }

        String status = (item.getItemSellStatus() != null) ? item.getItemSellStatus().name() : null;
        String category = (item.getCategory() != null) ? item.getCategory().name() : null;
        int price = toInt(item.getSalePrice());
        int stock = toInt(item.getStockNumber());

        ItemDetailView view = new ItemDetailView();
        view.id = item.getId();
        view.itemNm = item.getItemNm();
        view.price = price;
        view.stockNumber = stock;
        view.itemSellStatus = status;
        view.category = category;
        view.description = item.getItemDetail();
        view.repImgUrl = repUrl;
        view.images = thumbs;
        view.longImages = longImages;
        view.regTime = item.getRegTime();
        view.updateTime = item.getUpdateTime();

        return ResponseEntity.ok(view);
    }

    // --- 대표이미지 맵 조회 (itemId -> url) ---
    private Map<Long, String> loadRepImageMap(List<Long> ids) {
        TypedQuery<Object[]> tq = em.createQuery(
                "select ii.item.id, ii.imgUrl from ItemImage ii " +
                        "where ii.item.id in :ids and ii.repimgYn = true", Object[].class);
        List<Object[]> rows = tq.setParameter("ids", ids).getResultList();
        Map<Long, String> map = new HashMap<>();
        for (Object[] r : rows) {
            if (r[0] instanceof Long id && r[1] instanceof String url) map.put(id, url);
        }
        return map;
    }

    // --- 숫자 안전 변환 (JDK17, instanceof 패턴 없이) ---
    private static int toInt(Object n) {
        if (n == null) return 0;
        if (n instanceof Integer) return (Integer) n;
        if (n instanceof Long) return Math.toIntExact((Long) n);
        if (n instanceof Short) return ((Short) n).intValue();
        if (n instanceof Byte) return ((Byte) n).intValue();
        if (n instanceof Number) return ((Number) n).intValue();
        try { return Integer.parseInt(n.toString()); } catch (Exception e) { return 0; }
    }

    // ============ 응답 모델 ============
    public static class PageResponse<T> {
        public List<T> content;
        public long totalElements;
        public int totalPages;
        public int number;
        public int size;
        public PageResponse(List<T> content, long totalElements, int totalPages, int number, int size) {
            this.content = content; this.totalElements = totalElements;
            this.totalPages = totalPages; this.number = number; this.size = size;
        }
    }
    public static class MainItemCard {
        public Long id;
        public String itemNm;
        public String imgUrl;
        public Integer price;
        public Integer itemLike;
        public Integer itemViewCnt;
        public String category;
        public LocalDateTime regTime;
        public MainItemCard(Long id, String itemNm, String imgUrl, Integer price,
                            Integer itemLike, Integer itemViewCnt, String category, LocalDateTime regTime) {
            this.id = id; this.itemNm = itemNm; this.imgUrl = imgUrl; this.price = price;
            this.itemLike = itemLike; this.itemViewCnt = itemViewCnt; this.category = category; this.regTime = regTime;
        }
    }
    public static class ItemDetailView {
        public Long id;
        public String itemNm;
        public Integer price;
        public Integer stockNumber;
        public String itemSellStatus;
        public String category;
        public String description;
        public String repImgUrl;
        public List<ImgVM> images;
        public List<String> longImages;
        public LocalDateTime regTime;
        public LocalDateTime updateTime;
    }
    public static class ImgVM {
        public String imgUrl;
        public Boolean repImgYn;
        public ImgVM() {}
        public ImgVM(String imgUrl, Boolean repImgYn) { this.imgUrl = imgUrl; this.repImgYn = repImgYn; }
    }
}
