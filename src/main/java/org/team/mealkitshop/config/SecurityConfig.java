package org.team.mealkitshop.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.team.mealkitshop.security.handler.OAuth2LoginSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations())
                .requestMatchers("/favicon.ico");
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler formLoginSuccessHandler
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/thymeleaf/main",
                                "/login", "/signup", "/members/join",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico", "/mypage/withdraw/success",
                                "/member/socialjoin"
                        ).permitAll()
                        .requestMatchers("/oauth2/**").permitAll()

                        // 1:1 문의 관련 경로는 로그인 필요
                        .requestMatchers("/inquiry/my/**").authenticated()          // 사용자 로그인 필수
                        .requestMatchers("/inquiry/admin/**").hasRole("ADMIN")      // 관리자 전용

                        // 공지사항, FAQ, 이벤트
                        .requestMatchers("/board/list", "/board/read","/board/read/**").permitAll()
                        .requestMatchers("/board/register", "/board/modify", "/board/remove").hasRole("ADMIN")

                        // board 리뷰 게시판
                        .requestMatchers("/board/review/register", "/board/review/remove").hasRole("USER") // 글 작성/삭제는 USER
                        .requestMatchers("/board/review/reply/**").hasRole("ADMIN") // 댓글은 관리자만
                        .requestMatchers("/board/review/list", "/board/review/read/**").permitAll() // 조회는 모두 가능

                        // 리뷰 게시글 리액션
                        .requestMatchers("/board/reaction/review/**").authenticated()
                        .requestMatchers("/board/review/list-json").permitAll()

                        // TIP 게시판
                        .requestMatchers("/board/tip/list", "/board/tip/list-json", "/board/tip/read/**").permitAll()
                        .requestMatchers("/board/tip/register").hasRole("USER")
                        .requestMatchers("/board/tip/remove", "/board/tip/modify").hasRole("USER")

                        // ==========================
                        // 관리자용 일반 게시판 권한 추가
                        // /board/** 경로 중 작성/수정/삭제는 ADMIN 권한 필요
                        // 조회(list, read)는 모두 허용
                        // ==========================
                        .requestMatchers("/board/admin/register", "/board/register", "/board/modify", "/board/remove")
                        .hasRole("ADMIN")
                        .requestMatchers("/board/list", "/board/read/**").permitAll()

                        .requestMatchers("/mypage/**", "/cart/**", "/orders/**").authenticated()
                        .anyRequest().permitAll()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(formLoginSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .rememberMe(rm -> rm
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 14)
                )

                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        // ✅ 카카오 로그인 시작 시 항상 창 뜨도록 prompt=login 부착
                        .authorizationEndpoint(ae -> ae.authorizationRequestResolver(promptLoginResolver()))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/thymeleaf/main")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                )

                .requestCache(cache -> cache.requestCache(new HttpSessionRequestCache() {
                    @Override
                    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
                        String uri = request.getRequestURI();
                        if (uri != null && uri.startsWith("/mypage/withdraw")) {
                            // 탈퇴 관련 경로는 저장하지 않음
                            return;
                        }
                        super.saveRequest(request, response);
                    }
                }))

                // ==========================
                // AccessDeniedHandler 추가
                // ==========================
                .exceptionHandling(e -> e
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("text/html;charset=UTF-8");
                            String uri = request.getRequestURI();
                            String msg;
                            String redirect;

                            if(uri.startsWith("/board/tip")){
                                msg = "관리자는 팁게시판에 글을 작성할 수 없습니다";
                                redirect = "/board/tip/list";
                            }else if (uri.startsWith("/board/review")){
                                msg = "관리자는 리뷰를 작성할 수 없습니다.";
                                redirect = "/board/review/list";
                            }else{
                                msg = "접근 권한이 없습니다.";
                                redirect = "/thymeleaf/main";
                            }
                            response.getWriter().write(
                                    "<script>"+
                                            "alert('"+msg+"');"+
                                            "location.href='"+redirect+"';"+
                                            "</script>"
                            );
                        })
                )

                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /** Default resolver에 래핑해서, kakao 일 때만 prompt=login 추가 */
    @Bean
    public OAuth2AuthorizationRequestResolver promptLoginResolver() {
        DefaultOAuth2AuthorizationRequestResolver delegate =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest req = delegate.resolve(request);
                return maybeAddPrompt(req);
            }
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
                return maybeAddPrompt(req);
            }
            private OAuth2AuthorizationRequest maybeAddPrompt(OAuth2AuthorizationRequest req) {
                if (req == null) return null;
                // registration_id 읽어서 kakao 일 때만 prompt=login
                Object reg = req.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
                String regId = (reg == null) ? "" : reg.toString();
                if ("kakao".equalsIgnoreCase(regId)) {
                    Map<String, Object> extra = new LinkedHashMap<>(req.getAdditionalParameters());
                    extra.put("prompt", "login");
                    return OAuth2AuthorizationRequest.from(req)
                            .additionalParameters(extra)
                            .build();
                }
                return req;
            }
        };
    }

    /** 폼 로그인 성공 처리 (SavedRequest → prevPage → 메인) */
    @Bean
    public AuthenticationSuccessHandler formLoginSuccessHandler() {
        return new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {
                // 1) SavedRequest 먼저 처리하되, 탈퇴 경로면 무시
                SavedRequest saved = new HttpSessionRequestCache().getRequest(request, response);
                if (saved != null) {
                    String url = saved.getRedirectUrl();
                    if (isBadAfterLogin(url)) { // ⬅️ 탈퇴/로그인/로그아웃/회원가입 등 금지 경로
                        getRedirectStrategy().sendRedirect(request, response, "/thymeleaf/main");
                        return;
                    }
                    getRedirectStrategy().sendRedirect(request, response, url);
                    return;
                }

                // 2) prevPage 처리하되, 탈퇴 경로면 무시
                HttpSession session = request.getSession(false);
                String prev = (session != null) ? (String) session.getAttribute("prevPage") : null;
                if (prev == null || isBadAfterLogin(prev)) {
                    prev = "/thymeleaf/main";
                }
                getRedirectStrategy().sendRedirect(request, response, prev);
            }

            // ✅ 로그인 직후 절대 돌아가면 안 되는 경로 정의
            private boolean isBadAfterLogin(String url) {
                if (url == null) return true;
                return url.contains("/login")
                        || url.contains("/logout")
                        || url.contains("/signup")
                        || url.contains("/members/join")
                        || url.contains("/signup/success")
                        || url.contains("/oauth2/authorization")
                        || url.contains("/mypage/withdraw"); // ★ withdraw / withdraw/success 모두 걸러짐
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

}