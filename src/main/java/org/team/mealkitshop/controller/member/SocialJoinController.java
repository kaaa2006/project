package org.team.mealkitshop.controller.member;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.team.mealkitshop.dto.member.SocialJoinDTO;
import org.team.mealkitshop.service.member.MemberUpdateService;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class SocialJoinController {

    private final MemberUpdateService memberUpdateService;

    /** 소셜 온보딩 입력 폼 */
    @GetMapping("/socialjoin")
    public String socialJoinForm(Model model,
                                 HttpSession session,
                                 HttpServletResponse response,
                                 RedirectAttributes ra) {

        // ✅ 혹시 모를 자동 재인증(remember-me) 차단: 컨텍스트 비우고, remember-me 쿠키 제거
        SecurityContextHolder.clearContext();
        killCookie(response, "remember-me");

        // 세션 만료/직접 접근 차단
        Object email = session.getAttribute("SOCIAL_EMAIL");
        if (!(email instanceof String) || ((String) email).isBlank()) {
            ra.addFlashAttribute("error", "소셜 로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
            return "redirect:/login?error=social_flow_expired";
        }

        if (!model.containsAttribute("form")) {
            SocialJoinDTO form = new SocialJoinDTO();
            Object nm = session.getAttribute("SOCIAL_NAME");
            if (nm instanceof String s && !s.isBlank()) form.setMemberName(s);
            model.addAttribute("form", form);
        }

        model.addAttribute("emailPrefill", email);
        return "member/socialjoin";
    }

    /** 소셜 온보딩 제출 */
    @PostMapping("/socialjoin")
    public String socialJoinSubmit(@Valid @ModelAttribute("form") SocialJoinDTO form,
                                   BindingResult binding,
                                   HttpSession session,
                                   HttpServletRequest request,
                                   RedirectAttributes ra) {

        // 세션 만료/직접 POST 차단
        String emailFromSession = (String) session.getAttribute("SOCIAL_EMAIL");
        if (emailFromSession == null || emailFromSession.isBlank()) {
            ra.addFlashAttribute("error", "소셜 로그인 세션이 만료되었습니다. 다시 로그인해주세요.");
            return "redirect:/login?error=social_flow_expired";
        }

        // 검증 오류 시 폼으로 리다이렉트
        if (binding.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.form", binding);
            ra.addFlashAttribute("form", form);
            return "redirect:/member/socialjoin";
        }

        // 온보딩 데이터 저장
        memberUpdateService.completeSocialJoinByEmail(emailFromSession, form);

        // 온보딩용 세션 정보 제거
        session.removeAttribute("SOCIAL_EMAIL");
        session.removeAttribute("SOCIAL_NAME");
        session.removeAttribute("SOCIAL_PENDING");

        // ✅ 비로그인 상태로 가입완료 페이지를 보여주기 위해 인증/세션 완전 정리
        SecurityContextHolder.clearContext();
        try {
            HttpSession s = request.getSession(false);
            if (s != null) s.invalidate();
        } catch (IllegalStateException ignore) {}

        // ✅ 로그인 없이 뷰 렌더링 (상단바가 비로그인 상태로 표시됨)
        return "member/signup-success";
    }

    /* --------- cookie helper --------- */
    private void killCookie(HttpServletResponse res, String name) {
        Cookie c = new Cookie(name, "");
        c.setPath("/");
        c.setMaxAge(0);
        c.setHttpOnly(true);
        res.addCookie(c);
    }
}