package org.team.mealkitshop.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.dto.member.MemberAdminDetailDTO;
import org.team.mealkitshop.service.member.MemberService;

@Controller
@RequiredArgsConstructor
public class AdminViewController {

    private final MemberService memberService;

    @GetMapping("/admin")
    public String adminHome() { return "admin/dashboard"; }

    @GetMapping("/admin/members")
    public String adminMembers(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "sort", defaultValue = "desc") String sort, // desc/asc/grade
                               @PageableDefault(size = 30) Pageable pageable, // 기본 30개/페이지
                               Model model) {

        Page<MemberAdminDetailDTO> members =
                memberService.getAdminMemberList(keyword, category, sort, pageable);

        model.addAttribute("members", members);
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);

        // ✅ 동적 드롭다운(등급/역할)
        model.addAttribute("grades", Grade.values());
        model.addAttribute("roles", Role.values());

        // 상태/소셜은 고정 셋을 유지(원하면 enum로 바꿔줄 수 있음)
        model.addAttribute("statuses", new Status[]{Status.ACTIVE, Status.WITHDRAWN});
        // LOCAL/KAKAO/NAVER은 템플릿에서 고정 옵션 유지

        return "admin/members";
    }

    // ✅ 수정: 이름/역할/등급 변경 (+ 리다이렉트 시 쿼리 파라미터 보존)
    @PostMapping("/admin/members/{mno}/update")
    public String updateMember(@PathVariable Long mno,
                               @RequestParam("memberName") String memberName,
                               @RequestParam("role") String role,
                               @RequestParam("grade") String grade,
                               @RequestParam(value = "points", required = false) Integer points, // ✅ 추가
                               // 파라미터 보존
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "sort", defaultValue = "desc") String sort,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "30") int size,
                               RedirectAttributes ra) {

        memberService.adminUpdateMemberBasic(
                mno,
                memberName,
                Role.valueOf(role.toUpperCase()),
                Grade.valueOf(grade.toUpperCase())
        );

        // ✅ 포인트도 저장 (null이 아니고 0 이상일 때)
        if (points != null && points >= 0) {
            memberService.adminUpdateMemberPoints(mno, points);
        }

        ra.addFlashAttribute("msg", "회원 정보가 수정되었습니다.");

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/members")
                .queryParam("keyword", keyword)
                .queryParam("category", category)
                .queryParam("sort", sort)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        return "redirect:" + redirectUrl;
    }

    // ✅ 삭제: 상태 변경 or 물리삭제(DROP) (+ 파라미터 보존)
    @PostMapping("/admin/members/{mno}/delete")
    public String deleteMember(@PathVariable Long mno,
                               @RequestParam("mode") String mode, // "status" or "DROP"
                               @RequestParam(value = "status", required = false) String status,
                               // 파라미터 보존용
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "sort", defaultValue = "desc") String sort,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "30") int size,
                               RedirectAttributes ra) {

        boolean drop = "DROP".equalsIgnoreCase(mode);
        if (drop) {
            memberService.adminDeleteOrWithdraw(mno, true, null);
            ra.addFlashAttribute("msg", "회원이 삭제(DROP)되었습니다.");
        } else {
            Status st = Status.valueOf(status.toUpperCase()); // ACTIVE / WITHDRAWN
            memberService.adminDeleteOrWithdraw(mno, false, st);
            ra.addFlashAttribute("msg", "회원 상태가 " + st + "(으)로 변경되었습니다.");
        }

        String redirectUrl = UriComponentsBuilder.fromPath("/admin/members")
                .queryParam("keyword", keyword)
                .queryParam("category", category)
                .queryParam("sort", sort)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

        return "redirect:" + redirectUrl;
    }
}
