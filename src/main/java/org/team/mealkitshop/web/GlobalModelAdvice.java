package org.team.mealkitshop.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.Map;
import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final MemberRepository memberRepository;

    public record LoginMemberSummary(String name, Grade grade) {}

    @ModelAttribute("loginMember")
    public LoginMemberSummary addLoginMemberToModel() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        String email = null;
        String socialNameAttr = null;

        // 1) 로컬 로그인 (UserDetails)
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername(); // 우리 프로젝트에선 이메일
        }
        // 2) 소셜 로그인 (OAuth2User)
        else if (principal instanceof OAuth2User ou) {
            Map<String, Object> attrs = ou.getAttributes();
            Object em = attrs.get("email");
            if (em instanceof String s && !s.isBlank()) {
                email = s;
            }
            // 소셜 닉네임(폴백용)
            Object nm = attrs.get("name");
            if (nm instanceof String s2 && !s2.isBlank()) {
                socialNameAttr = s2;
            }
        } else {
            // anonymousUser 등
            return null;
        }

        // 이메일이 있어야 DB 조회 가능
        if (email == null || email.isBlank()) {
            // 이메일이 없으면 소셜 닉네임이라도 표기하고 그 외엔 숨김
            if (socialNameAttr != null) {
                return new LoginMemberSummary(socialNameAttr, Grade.BASIC);
            }
            return null;
        }

        Optional<Member> opt = memberRepository.findByEmail(email);
        if (opt.isEmpty()) {
            // DB에 아직 없는데 소셜 name만 있는 경우 폴백
            if (socialNameAttr != null) {
                return new LoginMemberSummary(socialNameAttr, Grade.BASIC);
            }
            return null;
        }

        Member m = opt.get();
        String name = (m.getMemberName() != null && !m.getMemberName().isBlank())
                ? m.getMemberName()
                : (socialNameAttr != null ? socialNameAttr : "고객");

        Grade grade = (m.getGrade() != null) ? m.getGrade() : Grade.BASIC;

        return new LoginMemberSummary(name, grade);
    }
}