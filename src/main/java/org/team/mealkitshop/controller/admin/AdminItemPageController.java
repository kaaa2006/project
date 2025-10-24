// src/main/java/org/team/mealkitshop/controller/admin/AdminItemPageController.java
package org.team.mealkitshop.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.common.Category;
import org.team.mealkitshop.common.FoodItem;
import org.team.mealkitshop.common.ItemSellStatus;
import org.team.mealkitshop.common.ItemSortType;
import org.team.mealkitshop.config.ItemDeletePolicyProperties;
import org.team.mealkitshop.dto.item.*;
import org.team.mealkitshop.repository.order.OrderItemRepository;
import org.team.mealkitshop.service.item.ItemImgService;
import org.team.mealkitshop.service.item.ItemService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Log4j2
public class AdminItemPageController {

    private final ItemService itemService;
    private final ItemImgService itemImgService;
    private final OrderItemRepository orderItemRepository;
    private final ItemDeletePolicyProperties deletePolicy;

    /** ✅ 관리자 상품 목록 페이지 */
    @GetMapping({"", "/"})
    public String listPage(
            @RequestParam(required = false) String searchQuery,
            @RequestParam(required = false) String itemSellStatus,
            @RequestParam(required = false) String category,                 // 문자열로 받아서 안전 변환
            @RequestParam(defaultValue = "NEW") ItemSortType sort,
            @PageableDefault(size = 20) Pageable pageable,
            Model model
    ) {
        ItemSearchDTO cond = new ItemSearchDTO();
        cond.setSearchBy("itemNm");
        cond.setSearchQuery(searchQuery);
        cond.setKeyword(searchQuery);
        if (StringUtils.hasText(itemSellStatus)) {
            try { cond.setItemSellStatus(ItemSellStatus.valueOf(itemSellStatus)); } catch (IllegalArgumentException ignore) {}
        }
        if (StringUtils.hasText(category)) {
            try { cond.setCategory(Category.valueOf(category)); } catch (IllegalArgumentException ignore) {}
        }
        cond.setSortType(sort);

        Page<ListItemDTO> page = itemService.getAdminPage(cond, pageable);

        model.addAttribute("page", page);
        model.addAttribute("categories", Category.values());
        model.addAttribute("selectedStatus", itemSellStatus);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("sort", sort);
        return "admin/items/list"; // templates/admin/items/list.html
    }

    /** 상품 등록 폼 */
    @GetMapping("/new")
    public String newItemPage(Model model) {
        model.addAttribute("form", new ItemFormDTO());
        return "admin/items/new";
    }

    /** 상품 등록 처리 (페이지 폼 제출) */
    @PostMapping(value = "/new", consumes = "multipart/form-data")
    public String createFromPage(@Valid @ModelAttribute("form") ItemFormDTO form,
                                 BindingResult br,
                                 @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                 @RequestPart(value = "itemDetailFiles", required = false) List<MultipartFile> detailFiles,
                                 @RequestParam(value = "repIndex", required = false) Integer repIndex) throws IOException {
        if (br.hasErrors()) return "admin/items/new";

        Long id = itemService.create(form);

        // 상세 이미지 → DB 저장(detail=true) 후 본문에 <img> 삽입 + 즉시 업데이트
        if (detailFiles != null && !detailFiles.isEmpty()) {
            StringBuilder html = new StringBuilder(Optional.ofNullable(form.getItemDetail()).orElse(""));
            for (MultipartFile f : detailFiles) {
                if (f != null && !f.isEmpty()) {
                    ItemImgDTO d = itemImgService.createDetail(id, f);
                    html.append("<p><img loading=\"lazy\" src=\"")
                            .append(d.getImgUrl())
                            .append("\" alt=\"detail\"/></p>");
                }
            }
            form.setItemDetail(html.toString());
            itemService.update(id, form);
        }

        // 상품 이미지 저장 + 대표 지정
        if (files != null && !files.isEmpty()) {
            if (repIndex != null && repIndex >= 0 && repIndex < files.size()
                    && files.get(repIndex) != null && !files.get(repIndex).isEmpty()) {
                for (int i = 0; i < files.size(); i++) {
                    MultipartFile f = files.get(i);
                    if (f != null && !f.isEmpty()) {
                        itemImgService.create(id, f, i == repIndex);
                    }
                }
            } else {
                var valid = files.stream().filter(f -> f != null && !f.isEmpty()).toList();
                if (!valid.isEmpty()) itemImgService.saveImages(id, valid);
            }
        }

        return "redirect:/admin/items";
    }

    /** 수정 폼 */
    @GetMapping("/{id}/edit")
    public String editItemPage(@PathVariable Long id, Model model) {
        ItemDTO item = itemService.read(id);
        model.addAttribute("item", item);

        // 갤러리(상품) 이미지만
        model.addAttribute("itemImages", itemImgService.listProduct(id));

        // 상세(본문) 이미지만 (imgName 이 "detail/" 로 시작하는 것만 필터)
        model.addAttribute("detailImages",
                itemImgService.list(id).stream()
                        .filter(d -> {
                            String name = d.getImgName();
                            return name != null && name.startsWith("detail/");
                        })
                        .toList()
        );
        // 상세 이미지 URL 목록(에디터 초기화/검증 등에 사용)
        model.addAttribute("detailImageUrls", itemImgService.listDetailUrls(id));

        ItemFormDTO form = ItemFormDTO.of(item);
        model.addAttribute("form", form);

        boolean hasOrders = orderItemRepository.existsByItem_Id(id);
        model.addAttribute("hasOrders", hasOrders);
        model.addAttribute("retentionDays", deletePolicy.retentionDays());
        return "admin/items/edit";
    }

    /** 수정 처리(간단) */
    @PostMapping("/{id}/edit")
    public String updateFromPage(@PathVariable Long id,
                                 @Valid @ModelAttribute("form") ItemFormDTO form,
                                 BindingResult br,
                                 Model model) {
        if (br.hasErrors()) {
            model.addAttribute("item", itemService.read(id));
            return "admin/items/edit";
        }
        itemService.update(id, form);
        return "redirect:/admin/items/" + id;
    }

    /** 상세 페이지 */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.read(id));
        model.addAttribute("itemId", id);
        return "admin/items/detail";
    }

    /* 공통 enum 바인딩 */
    @ModelAttribute("foodItems")
    public FoodItem[] foodItems() { return FoodItem.values(); }

    @ModelAttribute("sellStatuses")
    public ItemSellStatus[] sellStatuses() { return ItemSellStatus.values(); }
}
