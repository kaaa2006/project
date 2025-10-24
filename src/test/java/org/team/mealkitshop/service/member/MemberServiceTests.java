package org.team.mealkitshop.service.member;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.service.MemberService;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(false)
class MemberServiceTests {

    @Autowired
    MemberService memberService;

    @Test
    void existsByEmail_and_saveMember_works() {
        String email = "TempUser@Local.COM"; // 대문자 포함 → normalize 확인
        assertFalse(memberService.existsByEmail(email));

        Member m = Member.builder()
                .email(email)
                .password("pass1234!") // raw
                .memberName("홍길동")
                .phone("01012345678")
                .build();

        Member saved = memberService.saveMember(m);

        // 비번이 bcrypt로 인코딩됐는지
        assertTrue(saved.getPassword().matches("^\\$2[aby]\\$.*"));

        // 기본값들 세팅됐는지
        assertEquals(Provider.Local, saved.getProvider());
        assertEquals(Role.USER, saved.getRole());
        assertEquals(Status.ACTIVE, saved.getStatus());
        assertEquals(Grade.BASIC, saved.getGrade());
        assertEquals(0, saved.getPoints());

        // 이메일 정규화 후 중복 체크되는지
        assertTrue(memberService.existsByEmail("tempuser@local.com")); // 소문자
    }

}