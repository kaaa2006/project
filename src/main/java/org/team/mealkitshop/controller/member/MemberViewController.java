package org.team.mealkitshop.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.dto.member.MemberJoinDTO;     // ✅ 외부 DTO 사용 (AssertTrue 포함)
import org.team.mealkitshop.dto.member.SocialJoinDTO; // ✅ 조회용 서비스
import org.team.mealkitshop.service.member.MemberService;       // ✅ 저장/인증용 서비스


@Controller
@RequiredArgsConstructor

@Validated
public class MemberViewController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        Model model,
                        HttpServletRequest request,
                        HttpSession session) {
        model.addAttribute("error", error != null);
        String ref = request.getHeader("Referer");
        if (ref != null && !ref.contains("/login")) {
            session.setAttribute("prevPage", ref);
        }
        return "member/login";
    }

    // /signup 랜딩(소셜/일반 선택)
    @GetMapping("/signup")
    public String signupLanding() {
        return "member/signup";
    }

    @GetMapping("/members/join")
    public String joinForm(Model model) {
        model.addAttribute("memberJoinDTO", new MemberJoinDTO());
        return "member/signup-form";
    }

    @PostMapping("/members/join")
    public String joinSubmit(@Valid @ModelAttribute("memberJoinDTO") MemberJoinDTO dto,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {

        // 1) Bean Validation (@AssertTrue로 비번 일치 검증 포함)
        if (bindingResult.hasErrors()) {
            return "member/signup-form";
        }

        // 2) 중복 이메일 검사
        if (memberService.existsByEmail(dto.getEmail())) {
            bindingResult.addError(new FieldError(
                    "memberJoinDTO", "email", dto.getEmail(), false,
                    new String[]{"duplicate"}, null, "이미 사용 중인 이메일입니다."
            ));
            return "member/signup-form";
        }

        // 3) 전화번호 정규화(숫자만 남김) — 필요 없으면 제거해도 됨
        String normalizedPhone = dto.getPhone() == null ? null : dto.getPhone().replaceAll("[^0-9]", "");

        // 4) 엔티티 생성 (marketingYn은 boolean → isMarketingYn())
        Member member = Member.builder()
                .email(dto.getEmail())
                .password(dto.getPassword())     // 저장 시 Service에서 bcrypt 인코딩
                .memberName(dto.getMemberName())
                .phone(normalizedPhone)
                .grade(Grade.BASIC)
                .points(0)
                .role(Role.USER)
                .provider(Provider.Local)
                .status(Status.ACTIVE)
                .marketingYn(dto.isMarketingYn()) // ✅ boolean 게터
                .build();

        memberService.saveMember(member);
        redirectAttributes.addFlashAttribute("email", dto.getEmail());
        return "redirect:/signup/success";
    }

    @GetMapping("/signup/success")
    public String signupSuccess() {
        return "member/signup-success";
    }

}
