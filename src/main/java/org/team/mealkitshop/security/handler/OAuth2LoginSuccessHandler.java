package org.team.mealkitshop.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.config.CustomUserDetails;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res,
                                        Authentication authentication) throws IOException {
        OAuth2User u = (OAuth2User) authentication.getPrincipal();

        String registrationId = null;
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId(); // "kakao" / "naver"
        }

        Extracted e = extract(registrationId, u.getAttributes());
        if (isBlank(e.email)) {
            res.sendRedirect("/login?error=social_email_required");
            return;
        }

        Provider provider = mapProvider(registrationId);

        Optional<Member> opt = memberRepository.findByEmail(e.email.trim().toLowerCase());
        final boolean isNew = opt.isEmpty();

        Member m = opt.orElseGet(() -> {
            Member nm = new Member();
            nm.setEmail(e.email.trim().toLowerCase());
            nm.setMemberName(makeNickname(e.name, e.email));
            nm.setProvider(provider);
            nm.setRole(Role.USER);
            nm.setStatus(Status.ACTIVE);
            nm.setGrade(Grade.BASIC);
            nm.setPoints(0);
            nm.setMarketingYn(false);
            nm.setPassword(passwordEncoder.encode("SOCIAL-" + UUID.randomUUID()));
            nm.setPhone("");
            return nm;
        });

        if (isNew) {
            try {
                memberRepository.save(m);
            } catch (DataIntegrityViolationException ex) {
                m.setMemberName(makeNickname(e.name + "-" + shortRand(), e.email));
                memberRepository.save(m);
            }
        } else {
            if (m.getProvider() == null) m.setProvider(provider);
            if (isBlank(m.getMemberName()) && !isBlank(e.name)) m.setMemberName(e.name);
        }

        boolean needExtra = isNew || isBlank(m.getPhone());

        if (needExtra) {
            // ✅ 1) 현재 인증/세션을 깨끗이 정리
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.setInvalidateHttpSession(true);
            logoutHandler.logout(req, res, authentication);

            // ✅ 2) 새 세션 생성
            HttpSession fresh = req.getSession(true);
            fresh.setAttribute("SOCIAL_NAME", firstNonBlank(m.getMemberName(), e.name));
            fresh.setAttribute("SOCIAL_EMAIL", m.getEmail());

            // ✅ 3) 추가 입력 페이지로 이동
            res.sendRedirect("/member/socialjoin");
            return;
        } else {
            // ✅ CustomUserDetails 생성
            CustomUserDetails userDetails = new CustomUserDetails(m);

            // ✅ 인증 객체 생성 후 SecurityContext에 설정
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(newAuth);
            SecurityContextHolder.setContext(context);

            // ✅ 세션에 SecurityContext 저장
            HttpSession session = req.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", context);

            // ✅ 홈으로 이동
            res.sendRedirect("/thymeleaf/main");
        }
    }

    /* ---------- 속성 파싱 ---------- */
    private record Extracted(String email, String name) {}

    private Extracted extract(String registrationId, Map<String, Object> attr) {
        String email = val(attr, "email");
        String name  = val(attr, "name");

        if ("kakao".equalsIgnoreCase(registrationId)) {
            Map<String, Object> account = map(attr, "kakao_account");
            if (account != null) {
                if (isBlank(email)) email = val(account, "email");
                Map<String, Object> profile = map(account, "profile");
                if (profile != null && isBlank(name)) {
                    name = val(profile, "nickname");
                }
            }
        } else if ("naver".equalsIgnoreCase(registrationId)) {
            Map<String, Object> response = map(attr, "response");
            if (response != null) {
                if (isBlank(email)) email = val(response, "email");
                if (isBlank(name))  name  = val(response, "name");
            }
        }
        return new Extracted(email, firstNonBlank(name, localPart(email)));
    }

    /* ---------- Provider 안전 매핑 ---------- */
    private Provider mapProvider(String registrationId) {
        if ("naver".equalsIgnoreCase(registrationId)) {
            return tryEnums("NAVER", "Naver", "NaverEnum", "NAVER_PROVIDER");
        }
        return tryEnums("KAKAO", "Kakao", "KAKAO_PROVIDER");
    }

    private Provider tryEnums(String... names) {
        for (String n : names) {
            try { return Provider.valueOf(n); } catch (Exception ignore) {}
            try { return Provider.valueOf(n.toUpperCase()); } catch (Exception ignore) {}
        }
        try { return Provider.valueOf("Kakao"); } catch (Exception ignore) {}
        try { return Provider.valueOf("Naver"); } catch (Exception ignore) {}
        return Provider.valueOf(Provider.values()[0].name());
    }

    /* ---------- 유틸 ---------- */
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> src, String key) {
        Object v = src.get(key);
        return (v instanceof Map) ? (Map<String, Object>) v : null;
    }

    private String val(Map<String, Object> src, String key) {
        Object v = src.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private String localPart(String email) {
        if (isBlank(email)) return "user";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (b == null ? "" : b);
    }

    private String shortRand() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    private String makeNickname(String name, String email) {
        String base = firstNonBlank(name, localPart(email));
        return base + "-" + shortRand();
    }
}
