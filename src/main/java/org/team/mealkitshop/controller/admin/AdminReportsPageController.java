package org.team.mealkitshop.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminReportsPageController {

    @GetMapping("/admin/reports")
    public String reportsIndex() { return "admin/reports/index"; }

    @GetMapping("/admin/reports/exec")
    public String reportsExec() { return "admin/reports/exec"; }

    @GetMapping("/admin/reports/sales")
    public String reportsSales() { return "admin/reports/sales"; }
}
