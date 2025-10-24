package org.team.mealkitshop.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.dto.item.ItemDTO;
import org.team.mealkitshop.dto.item.ItemFormDTO;
import org.team.mealkitshop.dto.item.ItemImgDTO;
import org.team.mealkitshop.repository.order.OrderItemRepository;
import org.team.mealkitshop.service.item.ItemImgService;
import org.team.mealkitshop.service.item.ItemService;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping(value = "/api/admin/items", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Log4j2
public class AdminItemController {

    private final ItemService itemService;
    private final ItemImgService itemImgService;
    private final OrderItemRepository orderItemRepository;

    /* -------------------- 생성(new.html: FormData) -------------------- */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @ModelAttribute ItemFormDTO form,
            @RequestPart(value = "files",           required = false) List<MultipartFile> files,
            @RequestPart(value = "itemDetailFiles", required = false) List<MultipartFile> detailFiles,
            @RequestParam(value = "repIndex",       required = false) Integer repIndex
    ) throws IOException {

        // 1) 아이템 생성
        Long id = itemService.create(form);

        // 2) 상세 이미지(detail=true) 저장 → URL을 본문 뒤에 삽입 → 본문만 패치 업데이트
        if (detailFiles != null && !detailFiles.isEmpty()) {
            StringBuilder html = new StringBuilder(Optional.ofNullable(form.getItemDetail()).orElse(""));
            for (MultipartFile f : filter(detailFiles)) {
                ItemImgDTO d = itemImgService.createDetail(id, f);
                html.append("<p><img loading=\"lazy\" src=\"")
                        .append(d.getImgUrl()).append("\" alt=\"detail\"/></p>");
            }
            ItemFormDTO patch = copyForDetailOnly(form, html.toString());
            itemService.update(id, patch);
        }

        // 3) 갤러리(상품) 이미지 저장 + 대표 지정
        saveProductImages(id, files, repIndex);

        return ResponseEntity.created(URI.create("/admin/items/" + id))
                .body(Map.of("id", id));
    }

    /* -------------------- 수정(edit.js: PUT + FormData) -------------------- */
    // JS가 FormData(멀티파트)로 보내므로 멀티파트 PUT 지원
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMultipart(@PathVariable Long id, @ModelAttribute ItemFormDTO form) {
        // 에디터 상세 이미지는 별도 업로드 API를 사용 → 여기서는 본문/기본 필드만 갱신
        if (!StringUtils.hasText(form.getItemDetail())) {
            ItemDTO cur = itemService.read(id);
            form.setItemDetail(cur.getItemDetail());
        }
        itemService.update(id, form);
        return ResponseEntity.noContent().build();
    }

    /* -------------------- 상세 이미지 업로드 (붙여넣기/드롭/버튼) -------------------- */
    // edit.js: POST /{id}/detail-images -> { urls: [...] }
    @PostMapping(path = "/{id}/detail-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDetailImages(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files
    ) throws IOException {
        List<String> urls = new ArrayList<>();
        for (MultipartFile f : filter(files)) {
            urls.add(itemImgService.createDetail(id, f).getImgUrl());
        }
        return ResponseEntity.ok(Map.of("urls", urls));
    }

    /* -------------------- 갤러리 이미지 업로드 + 대표 인덱스 -------------------- */
    // edit.js: POST /{id}/images (repIndex optional)
    @PostMapping(path = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadProductImages(
            @PathVariable Long id,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "repIndex", required = false) Integer repIndex
    ) throws IOException {
        List<ItemImgDTO> added = saveProductImages(id, files, repIndex);
        List<String> urls = added.stream().map(ItemImgDTO::getImgUrl).filter(Objects::nonNull).toList();
        return ResponseEntity.ok(Map.of("urls", urls));
    }

    /* 대표 지정(상세 이미지는 금지) */
    @PostMapping("/{itemId}/images/{imageId}/rep")
    public ResponseEntity<Void> setRepresentative(@PathVariable Long itemId, @PathVariable Long imageId) {
        itemImgService.setRepresentative(itemId, imageId);
        return ResponseEntity.noContent().build();
    }

    /* 개별 이미지 삭제(상품/상세 공통, 소유검증 포함) */
    @DeleteMapping("/{itemId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long itemId, @PathVariable Long imageId) {
        itemImgService.deleteWithOwnershipCheck(itemId, imageId);
        return ResponseEntity.noContent().build();
    }

    /* 선택: 단건 조회(디버그/관리 화면용) */
    @GetMapping("/{id}")
    public ItemDTO getOne(@PathVariable Long id) { return itemService.read(id); }

    /* 정책 삭제(soft/hard/hard-forced, 헤더로 결과 통지) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestParam(defaultValue = "false") boolean forceStopOnly) {
        String result = itemService.deleteWithPolicy(id, forceStopOnly);
        return switch (result) {
            case "soft" -> ResponseEntity.noContent()
                    .header("X-Delete-Result", "soft")
                    .header("X-Delete-Message", "판매중지(STOP)로 전환되었습니다. 보존기간 경과 후 삭제 가능합니다.")
                    .build();
            case "hard" -> ResponseEntity.noContent()
                    .header("X-Delete-Result", "hard")
                    .header("X-Delete-Message", "영구 삭제되었습니다.")
                    .build();
            case "hard-forced" -> ResponseEntity.noContent()
                    .header("X-Delete-Result", "hard-forced")
                    .header("X-Delete-Message", "강제 영구 삭제되었습니다.")
                    .build();
            default -> ResponseEntity.status(409).body(Map.of(
                    "message", "보존기간이 남았거나 주문 이력이 있어 삭제할 수 없습니다."
            ));
        };
    }

    /* ================== 내부 유틸 ================== */

    @GetMapping("/{id}/order-exists")
    public ResponseEntity<Boolean> orderExists(@PathVariable Long id) {
        boolean exists = orderItemRepository.existsByItem_Id(id);
        return ResponseEntity.ok(exists);
    }

    private static List<MultipartFile> filter(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private ItemFormDTO copyForDetailOnly(ItemFormDTO src, String newHtml) {
        ItemFormDTO d = new ItemFormDTO();
        d.setItemNm(src.getItemNm());
        d.setOriginalPrice(src.getOriginalPrice());
        d.setDiscountRate(src.getDiscountRate());
        d.setStockNumber(src.getStockNumber());
        d.setItemSellStatus(src.getItemSellStatus());
        d.setFoodItem(src.getFoodItem());
        d.setItemDetail(newHtml);
        return d;
    }

    private List<ItemImgDTO> saveProductImages(Long id, List<MultipartFile> files, Integer repIndex) throws IOException {
        List<MultipartFile> valid = filter(files);
        if (valid.isEmpty()) return List.of();

        // 명시적 대표 인덱스가 유효하면 그 파일을 대표로
        if (repIndex != null && files != null
                && repIndex >= 0 && repIndex < files.size()
                && files.get(repIndex) != null && !files.get(repIndex).isEmpty()) {
            List<ItemImgDTO> out = new ArrayList<>();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile f = files.get(i);
                if (f != null && !f.isEmpty()) out.add(itemImgService.create(id, f, i == repIndex));
            }
            return out;
        }
        // 아니면 서비스에서 자동 대표 지정
        return itemImgService.saveImages(id, valid);
    }
}
