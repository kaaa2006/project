package org.team.mealkitshop.component;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.domain.board.ReviewBoard;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.repository.member.MemberRepository;

@Component
@RequiredArgsConstructor
public class Rq {

    private final MemberRepository memberRepository;

    /** 로그인 여부 확인 */
    public boolean isLogined() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && !(auth instanceof AnonymousAuthenticationToken) && auth.isAuthenticated();
    }

    /** 로그인한 회원 ID 반환, 없으면 null */
    public String getMemberId() {
        if (!isLogined()) return null;
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /** 로그인된 Member 객체 반환, 없으면 예외 */
    public Member getMember() {
        String email = getMemberId();
        if (email == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인된 회원을 DB에서 찾을 수 없습니다."));
    }

    /** 로그인 안 했으면 null 반환 */
    public Member getMemberOrNull() {
        if (!isLogined()) return null;
        return memberRepository.findByEmail(getMemberId())
                .orElse(null);
    }

    /** 로그인 필수 체크, 없으면 예외 */
    public Member mustGetMember() {
        Member member = getMember();
        if (member == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        return member;
    }

    // ✅ 여기 추가
    public boolean isAdmin() {
        Member member = getMemberOrNull();
        return member != null && member.getRole() == Role.ADMIN;
    }

    // 로그인 회원 PK(Long) 반환
    public Long getMemberMno() {
        Member member = getMember();
        return member.getMno();
    }

    /** 특정 게시글에 대해 로그인했거나, 관리자이거나, 작성자인지 확인 */
    public boolean isLoginedOrAdminOrWriter(ReviewBoard board) {
        if (!isLogined()) return false;      // 로그인 안했으면 false
        if (isAdmin()) return true;          // 관리자면 true

        Member member = getMemberOrNull();
        if (member == null) return false;

        // 이메일로 비교
        String writerEmail = board.getWriterMember() != null ? board.getWriterMember().getEmail().trim().toLowerCase() : "";
        String loginEmail = member.getEmail().trim().toLowerCase();

        return writerEmail.equals(loginEmail);
    }
}