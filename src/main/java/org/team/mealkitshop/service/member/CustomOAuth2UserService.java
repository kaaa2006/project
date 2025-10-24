package org.team.mealkitshop.service.member;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User raw = delegate.loadUser(req);

        String registrationId = req.getClientRegistration().getRegistrationId(); // kakao | naver
        Map<String, Object> attrs = raw.getAttributes();

        Map<String, Object> mapped = switch (registrationId.toLowerCase()) {
            case "kakao" -> mapKakao(attrs);
            case "naver" -> mapNaver(attrs);
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        };

        // 이메일이 없을 경우에도 nameAttributeKey에 넣을 안전한 값 필요
        String uniqueKey = mapped.get("email") != null
                ? String.valueOf(mapped.get("email"))
                : registrationId + "_" + UUID.randomUUID();

        // 디버깅 로그
        log.info("[OAUTH2 MAP] provider={}, mapped={}", registrationId, mapped);

        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // email 또는 fallback 키를 nameAttributeKey로 지정
        return new DefaultOAuth2User(authorities, mapped, "email");
    }

    private Map<String, Object> mapKakao(Map<String, Object> attrs) {
        Map<String, Object> kakaoAccount = safeMap(attrs.get("kakao_account"));
        Map<String, Object> profile      = safeMap(kakaoAccount.get("profile"));
        Map<String, Object> properties   = safeMap(attrs.get("properties"));

        String id    = String.valueOf(attrs.get("id"));
        String email = strOrNull(kakaoAccount.get("email"));
        String name  = firstNonBlank(
                strOrNull(profile.get("nickname")),
                strOrNull(properties.get("nickname")),
                emailLocalPart(email),
                "사용자"
        );

        Map<String, Object> m = new HashMap<>();
        m.put("provider", "kakao");
        m.put("email", email);
        m.put("name", name);
        m.put("kakaoId", id);
        return m;
    }

    private Map<String, Object> mapNaver(Map<String, Object> attrs) {
        Map<String, Object> resp = safeMap(attrs.get("response"));
        String email = strOrNull(resp.get("email"));
        String name  = firstNonBlank(strOrNull(resp.get("name")),
                emailLocalPart(email),
                "사용자");

        Map<String, Object> m = new HashMap<>();
        m.put("provider", "naver");
        m.put("email", email);
        m.put("name", name);
        m.put("naverId", strOrNull(resp.get("id")));
        return m;
    }

    // ===== helpers =====
    private Map<String, Object> safeMap(Object o) {
        return (o instanceof Map<?, ?> m)
                ? new HashMap<>((Map<String, Object>) m)
                : new HashMap<>();
    }

    private String strOrNull(Object o) {
        return (o == null) ? null : String.valueOf(o);
    }

    private String firstNonBlank(String... cands) {
        for (String s : cands) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    private String emailLocalPart(String email) {
        if (email == null) return null;
        int i = email.indexOf('@');
        return (i > 0) ? email.substring(0, i) : email;
    }
}
