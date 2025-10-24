package org.team.mealkitshop.controller.item;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/items")
public class ItemPageController {

    /** 목록 페이지: src/main/resources/templates/items/list.html */
    @GetMapping
    public String listPage(@RequestParam(required = false) String category,
                           @RequestParam(required = false) String foodItem,
                           @RequestParam(required = false) Boolean specialDeal,
                           @RequestParam(required = false) Boolean newItem,
                           Model model) {
        model.addAttribute("category", category);
        model.addAttribute("foodItem", foodItem != null ? foodItem : "");
        model.addAttribute("specialDeal", specialDeal != null ? specialDeal : false);
        model.addAttribute("newItem", newItem != null ? newItem : false);
        return "items/list";
    }


    /** 상세 페이지: src/main/resources/templates/items/detail.html */
    @GetMapping("/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        model.addAttribute("itemId", id);
        return "items/detail";
    }
}
