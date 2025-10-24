package org.team.mealkitshop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminUnderConstructionController {

    @GetMapping("/admin/under-construction")
    public String underConstruction() {
        return "under-construction"; // templates/under-construction.html
    }
}
