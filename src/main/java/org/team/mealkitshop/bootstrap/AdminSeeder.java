package org.team.mealkitshop.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"default","local","dev"}) // 운영(prod)에서는 자동 실행 안 됨
public class AdminSeeder implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        final String email = "admin@mealkit.shop";   // 로그인 ID
        final String name  = "sysadmin";             // 관리자 이름
        final String phone = "010-0000-0000";        // 필수 컬럼
        final String rawPw = "123456789";            // 초기 비밀번호 (운영 시 반드시 교체)

        memberRepository.findByEmail(email.toLowerCase())
                .ifPresentOrElse(
                        found -> log.info("[AdminSeeder] admin already exists: {}", found.getEmail()),
                        () -> {
                            Member admin = Member.builder()
                                    .email(email.toLowerCase())
                                    .memberName(name)
                                    .phone(phone)
                                    .password(passwordEncoder.encode(rawPw)) // ✅ 인코딩된 비밀번호
                                    .role(Role.ADMIN)
                                    .provider(Provider.Local)
                                    .status(Status.ACTIVE)
                                    .marketingYn(false)
                                    .build();

                            memberRepository.save(admin);
                            log.warn("[AdminSeeder] admin created: {} / tempPw={}", email, rawPw);
                        }
                );
    }
}
