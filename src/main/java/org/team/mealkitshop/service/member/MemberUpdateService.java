package org.team.mealkitshop.service.member;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.dto.member.MemberDeleteDTO;
import org.team.mealkitshop.dto.member.MemberUpdateDTO;
import org.team.mealkitshop.dto.member.SocialJoinDTO;
import org.team.mealkitshop.repository.member.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberUpdateService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /* =========================
     *  소셜 추가입력(온보딩)
     * ========================= */
    @Transactional
    public void completeSocialJoin(Long memberId, SocialJoinDTO dto) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (isBlank(dto.getPassword())) throw new IllegalArgumentException("비밀번호를 입력하세요.");
        m.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (isBlank(dto.getMemberName())) throw new IllegalArgumentException("이름을 입력하세요.");
        m.setMemberName(dto.getMemberName());

        if (isBlank(dto.getPhone())) throw new IllegalArgumentException("휴대폰번호를 입력하세요.");
        m.setPhone(dto.getPhone().replaceAll("[^0-9]", ""));

        m.setMarketingYn(dto.isMarketingYn());

        if (m.getProvider() == null) m.setProvider(Provider.Kakao);
        if (m.getRole() == null)     m.setRole(Role.USER);
        if (m.getStatus() == null)   m.setStatus(Status.ACTIVE);
        if (m.getGrade() == null)    m.setGrade(Grade.BASIC);
    }

    @Transactional
    public void completeSocialJoinByEmail(String email, SocialJoinDTO dto) {
        if (isBlank(email)) throw new IllegalArgumentException("이메일이 없습니다. 다시 로그인해주세요.");

        Member m = memberRepository.findByEmail(email).orElseGet(() -> {
            Member nm = new Member();
            nm.setEmail(email);
            nm.setProvider(Provider.Kakao);
            nm.setRole(Role.USER);
            nm.setStatus(Status.ACTIVE);
            nm.setGrade(Grade.BASIC);
            nm.setPoints(0);
            return nm;
        });

        if (isBlank(dto.getPassword())) throw new IllegalArgumentException("비밀번호를 입력하세요.");
        m.setPassword(passwordEncoder.encode(dto.getPassword()));

        if (isBlank(dto.getMemberName())) throw new IllegalArgumentException("이름을 입력하세요.");
        m.setMemberName(dto.getMemberName());

        if (isBlank(dto.getPhone())) throw new IllegalArgumentException("휴대폰번호를 입력하세요.");
        m.setPhone(dto.getPhone().replaceAll("[^0-9]", ""));

        m.setMarketingYn(dto.isMarketingYn());

        memberRepository.save(m);
    }

    /* =========================
     *  회원 프로필/비밀번호/마케팅 (주소 제외)
     * ========================= */
    @Transactional
    public void updateProfile(Long memberId, MemberUpdateDTO memberDto) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (notBlank(memberDto.getPhone())) {
            m.setPhone(memberDto.getPhone());
        }
        m.setMarketingYn(memberDto.isMarketingYn());

        if (anyPasswordFieldFilled(memberDto)) {
            validatePasswordBlock(memberDto);
            if (!passwordEncoder.matches(memberDto.getCurrentPassword(), m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            m.setPassword(passwordEncoder.encode(memberDto.getNewPassword()));
        }
    }

    /* =========================
     *  탈퇴
     * ========================= */
    @Transactional
    public void withdraw(Long memberId, MemberDeleteDTO dto) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        if (m.getProvider() == Provider.Local) {
            if (dto == null || !notBlank(dto.getCurrentPassword())) {
                throw new IllegalArgumentException("현재 비밀번호를 입력하세요.");
            }
            if (!passwordEncoder.matches(dto.getCurrentPassword(), m.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
        }

        m.setStatus(Status.WITHDRAWN);
        m.setMarketingYn(false);

        // 실제 운영에선 unique 제약/복구정책 등 고려 필요
        m.setEmail("withdrawn_" + m.getMno() + "@deleted.local");
        m.setMemberName("탈퇴회원" + m.getMno());
    }

    /* =========================
     *  헬퍼
     * ========================= */
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private boolean anyPasswordFieldFilled(MemberUpdateDTO d) {
        return notBlank(d.getCurrentPassword())
                || notBlank(d.getNewPassword())
                || notBlank(d.getNewPasswordConfirm());
    }

    private void validatePasswordBlock(MemberUpdateDTO d) {
        if (!notBlank(d.getCurrentPassword())
                || !notBlank(d.getNewPassword())
                || !notBlank(d.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호 변경은 세 칸 모두 입력해야 합니다.");
        }
        if (!d.getNewPassword().equals(d.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 서로 일치하지 않습니다.");
        }
        if (d.getCurrentPassword().equals(d.getNewPassword())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 새 비밀번호를 입력하세요.");
        }
    }
}