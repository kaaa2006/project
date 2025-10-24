package org.team.mealkitshop.config;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.team.mealkitshop.domain.member.Member;

import java.util.Collection;
import java.util.Collections;

/**
 * [CustomUserDetails]
 * - Spring Security에서 인증된 사용자 정보를 담는 클래스
 * - Member 엔티티 기반으로 UserDetails 인터페이스를 구현
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberId;      // 내부 PK
    private final String email;       // 로그인 ID
    private final String password;    // 비밀번호 (BCrypt)
    private final String role;        // 권한 (USER / ADMIN)
    private final String memberName;  // 상품에서 추가

    public CustomUserDetails(Member member) {
        this.memberId = member.getMno();
        this.email = member.getEmail();
        this.password = member.getPassword();
        this.role = member.getRole().name(); // Enum → String
        this.memberName = member.getMemberName(); // 상품에서 추가 (리뷰 작성 시 작성자를 Mno가 아닌 memberName으로 출력하기 위함)
    }

    /** ✅ 권한 반환 (Spring Security는 ROLE_ prefix 필요) */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton((GrantedAuthority) () -> "ROLE_" + role);
    }

    /** ✅ 사용자 ID (로그인 아이디로 사용) */
    @Override
    public String getUsername() {
        return email;
    }

    /** ✅ 패스워드 반환 */
    @Override
    public String getPassword() {
        return password;
    }

    /** ✅ 계정 상태 관련 (필요 시 Member.status로 확장 가능) */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
