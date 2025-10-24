package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.domain.item.ItemImage;
import org.team.mealkitshop.dto.item.ItemImgDTO;
import org.team.mealkitshop.repository.item.ItemImgRepository;
import org.team.mealkitshop.repository.item.ItemRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 아이템 이미지 CRUD/조회 전담 서비스
 * - 상품(갤러리) 이미지: /images/item/**, detail=false
 * - 상세(본문) 이미지 : /images/detail/**, detail=true
 * - 대표이미지는 상품 이미지(detail=false)만 대상
 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ItemImgService {

    @Value("${imageBasePath:/images/}")
    private String imageBasePath;

    private final FileService fileService;
    private final ItemImgRepository itemImgRepository;
    private final ItemRepository itemRepository;

    /* ================= CREATE ================= */

    /** 상품 이미지 1장 업로드(+필요 시 대표 지정) */
    public ItemImgDTO create(Long itemId, MultipartFile file, boolean asRep) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("empty file");
        Item item = getItem(itemId);

        List<String> savedThisCall = new ArrayList<>();
        try {
            boolean makeRep = asRep || !hasRep(itemId); // detail=false 기준 대표가 없을 때 자동 대표
            if (makeRep) itemImgRepository.clearRep(itemId);

            ItemImage e = uploadAndMakeEntity(item, file, makeRep, savedThisCall);
            return toDTO(itemImgRepository.save(e));
        } catch (IOException | RuntimeException e) {
            cleanupSaved(savedThisCall);
            throw e;
        }
    }

    /** 상품 이미지 다중 업로드(처음 1장 자동 대표 지정 가능) */
    public List<ItemImgDTO> saveImages(Long itemId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return List.of();
        Item item = getItem(itemId);

        boolean alreadyHasRep = hasRep(itemId); // detail=false 기준
        boolean setOnce = false;
        List<ItemImage> batch = new ArrayList<>();
        List<String> savedThisBatch = new ArrayList<>();

        try {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                boolean makeRep = !alreadyHasRep && !setOnce;
                if (makeRep) {
                    itemImgRepository.clearRep(itemId);
                    setOnce = true;
                }
                batch.add(uploadAndMakeEntity(item, f, makeRep, savedThisBatch));
            }
            if (batch.isEmpty()) return List.of();
            return itemImgRepository.saveAll(batch).stream().map(this::toDTO).toList();
        } catch (IOException | RuntimeException e) {
            cleanupSaved(savedThisBatch);
            throw e;
        }
    }

    /** 상세(본문) 이미지 1장 업로드(detail=true, 본문에서 사용) */
    public ItemImgDTO createDetail(Long itemId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("empty file");
        Item item = getItem(itemId);

        String ori = file.getOriginalFilename();
        String saved;
        try (InputStream in = file.getInputStream()) {
            saved = fileService.uploadFileIn("detail", ori, in); // /detail 하위 저장
        }

        String imgName = "detail/" + saved;
        String imgUrl  = buildPublicUrl(imgName);

        ItemImage e = new ItemImage();
        e.updateItemImg(ori, imgName, imgUrl);
        e.markDetail(); // detail=true
        item.addImage(e);
        return toDTO(itemImgRepository.save(e));
    }

    /* ================= READ ================= */

    /** 상세(본문) 이미지 URL 목록(detail=true) */
    @Transactional(readOnly = true)
    public List<String> listDetailUrls(Long itemId) {
        return itemImgRepository.findByItemIdAndDetailTrueOrderByIdAsc(itemId)
                .stream().map(ItemImage::getImgUrl).toList();
    }

    /** 상품(갤러리) 이미지 목록(detail=false) */
    @Transactional(readOnly = true)
    public List<ItemImgDTO> listProduct(Long itemId) {
        return itemImgRepository.findByItemIdAndDetailFalseOrderByIdAsc(itemId)
                .stream().map(this::toDTO).toList();
    }

    /** 모든 이미지(상품+상세) — 하드삭제 등 내부용 */
    @Transactional(readOnly = true)
    public List<ItemImgDTO> list(Long itemId) {
        return itemImgRepository.findByItemIdOrderByIdAsc(itemId).stream()
                .map(this::toDTO)
                .toList();
    }

    /** 대표 이미지(상품용, detail=false만) */
    @Transactional(readOnly = true)
    public Optional<ItemImgDTO> getRepresentative(Long itemId) {
        return itemImgRepository.findByItemIdAndDetailFalseOrderByIdAsc(itemId).stream()
                .filter(img -> Boolean.TRUE.equals(img.getRepimgYn()))
                .findFirst()
                .map(this::toDTO);
    }

    /** 여러 아이템의 대표 이미지 맵(상품용, detail=false만) */
    @Transactional(readOnly = true)
    public Map<Long, ItemImgDTO> getRepresentatives(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyMap();
        return itemImgRepository.findByItem_IdInAndRepimgYnTrueAndDetailFalse(itemIds).stream()
                .collect(Collectors.toMap(img -> img.getItem().getId(), this::toDTO, (a, b) -> a));
    }

    /* ================= UPDATE ================= */

    /** 대표 이미지 지정(상세 이미지는 금지) */
    public void setRepresentative(Long itemId, Long imageId) {
        ItemImage target = itemImgRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));

        if (!Objects.equals(target.getItem().getId(), itemId)) {
            throw new IllegalArgumentException("Image does not belong to the item");
        }

        // 상세 이미지 금지: 파일 경로 규칙으로 판별 (detail/ 로 시작하면 상세 이미지)
        String name = Optional.ofNullable(target.getImgName()).orElse("");
        if (name.startsWith("detail/")) {
            throw new IllegalArgumentException("Detail image cannot be representative");
        }

        // 대표 초기화 후 지정 (detail=false 기준 대표만 유지)
        itemImgRepository.clearRep(itemId);
        int updated = itemImgRepository.setRep(imageId);
        if (updated == 0) {
            throw new IllegalStateException("Failed to set representative image: " + imageId);
        }
    }

    /* ================= DELETE ================= */

    /** 단일 이미지 삭제(대표 재지정 포함) */
    public void delete(Long imageId) {
        ItemImage e = itemImgRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
        try { fileService.deleteBySavedName(e.getImgName()); } catch (Exception ignore) {}
        Long itemId = e.getItem().getId();
        itemImgRepository.delete(e);
        ensureRep(itemId);
    }

    /** 다중 이미지 삭제(대표 재지정 포함) */
    public void deleteAll(Long itemId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;

        List<ItemImage> targets = new ArrayList<>(itemImgRepository.findAllById(ids));

        if (targets.size() != ids.size()) {
            throw new NoSuchElementException("Some images not found among: " + ids);
        }
        if (targets.stream().anyMatch(t -> !Objects.equals(t.getItem().getId(), itemId))) {
            throw new IllegalArgumentException("contains images of other item");
        }

        for (ItemImage t : targets) {
            try { fileService.deleteBySavedName(t.getImgName()); } catch (Exception ignore) {}
        }
        itemImgRepository.deleteAll(targets);
        ensureRep(itemId);
    }

    /* ================= Helpers ================= */

    /** 소유권 검증 후 삭제(대표 재지정 포함) */
    public void deleteWithOwnershipCheck(Long itemId, Long imageId) {
        ItemImage e = itemImgRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("Image not found: " + imageId));
        if (!Objects.equals(e.getItem().getId(), itemId)) {
            throw new IllegalArgumentException("Image does not belong to the item");
        }
        try { fileService.deleteBySavedName(e.getImgName()); } catch (Exception ignore) {}
        itemImgRepository.delete(e);
        ensureRep(itemId);
    }

    /** 아이템 로딩(이미지 연관 포함) */
    private Item getItem(Long id) {
        return itemRepository.findByIdWithImages(id)
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }

    /** detail=false 기준 대표 존재 여부 */
    private boolean hasRep(Long itemId) {
        return itemImgRepository.existsByItemIdAndRepimgYnTrueAndDetailFalse(itemId);
    }

    /** 대표 자동 지정: detail=false 중 첫 이미지 → 없으면 전체 중 첫 이미지(fallback) */
    private void ensureRep(Long itemId) {
        if (hasRep(itemId)) return;

        var products = itemImgRepository.findByItemIdAndDetailFalseOrderByIdAsc(itemId);
        if (!products.isEmpty()) {
            itemImgRepository.clearRep(itemId);
            itemImgRepository.setRep(products.get(0).getId());
            return;
        }

        var any = itemImgRepository.findByItemIdOrderByIdAsc(itemId);
        if (!any.isEmpty()) {
            itemImgRepository.clearRep(itemId);
            itemImgRepository.setRep(any.get(0).getId());
        }
    }

    /** 상품(갤러리) 이미지 업로드 내부 유틸(detail=false) */
    private ItemImage uploadAndMakeEntity(Item item, MultipartFile file, boolean asRep,
                                          List<String> savedTracker) throws IOException {
        String ori = Objects.requireNonNull(file.getOriginalFilename(), "original filename is null");
        String saved;
        try (InputStream in = file.getInputStream()) {
            saved = fileService.uploadFileIn("item", ori, in); // item/ 하위에 저장
        }
        if (savedTracker != null) savedTracker.add("item/" + saved);

        String imgName = "item/" + saved;            // 저장 파일명(DB)
        String imgUrl  = buildPublicUrl(imgName);    // 공개 URL

        ItemImage e = new ItemImage();
        e.updateItemImg(ori, imgName, imgUrl);      // detail=false 기본

        item.addImage(e);
        if (asRep) item.setRepresentative(e);
        return e;
    }

    /** 업로드 중 생성된 파일 롤백 정리 */
    private void cleanupSaved(List<String> savedNames) {
        for (String fn : savedNames) {
            try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {}
        }
    }

    /** 공개 URL 생성 */
    private String buildPublicUrl(String savedPath) {
        final String base = imageBasePath.endsWith("/") ? imageBasePath : imageBasePath + "/";
        return base + savedPath; // "detail/UUID.jpg" 또는 "item/UUID.jpg"
    }

    /** 엔티티 → DTO */
    private ItemImgDTO toDTO(ItemImage e) {
        return ItemImgDTO.builder()
                .id(e.getId())
                .imgName(e.getImgName())
                .oriImgName(e.getOriImgName())
                .imgUrl(e.getImgUrl())
                .repimgYn(Boolean.TRUE.equals(e.getRepimgYn()))
                .build();
    }
}
