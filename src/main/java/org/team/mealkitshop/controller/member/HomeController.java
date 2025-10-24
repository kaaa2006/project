package org.team.mealkitshop.controller.member;

import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.team.mealkitshop.common.ItemSortType;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.service.item.ItemService;

@Controller
@RequestMapping(value = "/thymeleaf")
@RequiredArgsConstructor // ← ItemService 생성자 주입
public class HomeController {

    private final ItemService itemService;

    @GetMapping("/main")
    public String main(Model model) {
        // 판매량 내림차순 TOP5
        ItemSearchDTO cond = new ItemSearchDTO();
        cond.setSortType(ItemSortType.SALES_DESC);

        var top5 = itemService
                .getListPage(cond, PageRequest.of(0, 5))
                .getContent();

        model.addAttribute("products", top5);
        return "thymeleaf/main"; // templates/thymeleaf/main.html
    }

    @GetMapping("/static")
    public String statictohome() {
        return "thymeleaf/main";
    }

    @GetMapping("/")
    public String Home() {
        return "redirect:/thymeleaf/main";
    }
}
