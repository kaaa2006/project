package org.team.mealkitshop.service.member;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.domain.member.Member;
import org.team.mealkitshop.domain.member.QMember;
import org.team.mealkitshop.dto.member.MemberAdminDetailDTO;
import org.team.mealkitshop.dto.member.MemberDetailDTO;
import org.team.mealkitshop.repository.member.MemberRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt

    @PersistenceContext
    private EntityManager em;

    /* ===== 회원 저장 ===== */
    @Transactional
    public Member saveMember(Member member) {
        String normalizedEmail = normalize(member.getEmail());
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("이메일이 비어있습니다.");
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }
        member.setEmail(normalizedEmail);

        String raw = member.getPassword();
        if (raw != null && !isBcryptHash(raw)) {
            member.setPassword(passwordEncoder.encode(raw));
        }

        if (member.getProvider() == null) member.setProvider(Provider.Local);
        if (member.getRole() == null) member.setRole(Role.USER);
        if (member.getStatus() == null) member.setStatus(Status.ACTIVE);
        if (member.getGrade() == null) member.setGrade(Grade.BASIC);
        if (member.getPoints() == null) member.setPoints(0);

        return memberRepository.save(member);
    }

    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(normalize(email));
    }

    public MemberDetailDTO getMemberDetail(String email) {
        String normalizedEmail = normalize(email);
        Member m = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다: " + normalizedEmail));

        return MemberDetailDTO.builder()
                .mno(m.getMno())
                .email(m.getEmail())
                .memberName(m.getMemberName())
                .phone(m.getPhone())
                .grade(m.getGrade())
                .points(m.getPoints() == null ? 0 : m.getPoints())
                .build();
    }

    private String normalize(String s) { return s == null ? null : s.trim().toLowerCase(); }
    private boolean isBcryptHash(String v) {
        return v != null && v.matches("\\A\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}\\z");
    }

    /* ===== 관리자 회원 리스트 (검색/카테고리/정렬: 최신/오래된/등급순) ===== */
    public Page<MemberAdminDetailDTO> getAdminMemberList(
            String keyword,
            String category,
            String sort,
            Pageable pageable) {

        QMember m = QMember.member;
        BooleanBuilder where = new BooleanBuilder();

        // 검색: 이름 or 이메일
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            where.and(m.memberName.containsIgnoreCase(kw)
                    .or(m.email.containsIgnoreCase(kw)));
        }

        // 카테고리: Role / Status / Grade / Provider
        if (category != null && !category.isBlank()) {
            String cat = category.trim();
            Role role = parseEnumIgnoreCase(Role.class, cat).orElse(null);
            if (role != null) where.and(m.role.eq(role));

            Status status = parseEnumIgnoreCase(Status.class, cat).orElse(null);
            if (status != null) where.and(m.status.eq(status));

            Grade grade = parseEnumIgnoreCase(Grade.class, cat).orElse(null);
            if (grade != null) where.and(m.grade.eq(grade));

            Provider provider = parseEnumIgnoreCase(Provider.class, cat).orElse(null);
            if (provider != null) where.and(m.provider.eq(provider));
        }

        // 정렬: grade | desc | asc
        if ("grade".equalsIgnoreCase(sort)) {
            JPAQueryFactory qf = new JPAQueryFactory(em);

            var gradeOrderExpr = new CaseBuilder()
                    .when(m.grade.eq(Grade.VIP)).then(1)
                    .when(m.grade.eq(Grade.GOLD)).then(2)
                    .when(m.grade.eq(Grade.SILVER)).then(3)
                    .when(m.grade.eq(Grade.BASIC)).then(4)
                    .otherwise(5);

            long total = Optional.ofNullable(
                    qf.select(m.count()).from(m).where(where).fetchOne()
            ).orElse(0L);

            var content = qf.selectFrom(m)
                    .where(where)
                    .orderBy(gradeOrderExpr.asc(), m.regTime.desc())
                    .offset(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .fetch();

            var dtoList = content.stream().map(e -> MemberAdminDetailDTO.builder()
                            .mno(e.getMno())
                            .createdAt(e.getRegTime())
                            .email(e.getEmail())
                            .memberName(e.getMemberName())
                            .phone(e.getPhone())
                            .grade(e.getGrade())
                            .role(e.getRole())
                            .points(e.getPoints() == null ? 0 : e.getPoints())
                            .provider(e.getProvider())
                            .status(e.getStatus())
                            .marketingYn(e.isMarketingYn())
                            .build())
                    .collect(Collectors.toList());

            return new PageImpl<>(dtoList, pageable, total);
        } else {
            Sort.Direction dir = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(dir, "regTime"));

            Page<Member> page = memberRepository.findAll(where, sorted);

            return page.map(e -> MemberAdminDetailDTO.builder()
                    .mno(e.getMno())
                    .createdAt(e.getRegTime())
                    .email(e.getEmail())
                    .memberName(e.getMemberName())
                    .phone(e.getPhone())
                    .grade(e.getGrade())
                    .role(e.getRole())
                    .points(e.getPoints() == null ? 0 : e.getPoints())
                    .provider(e.getProvider())
                    .status(e.getStatus())
                    .marketingYn(e.isMarketingYn())
                    .build());
        }
    }

    private static <E extends Enum<E>> Optional<E> parseEnumIgnoreCase(Class<E> type, String text) {
        if (text == null) return Optional.empty();
        for (E e : type.getEnumConstants()) {
            if (e.name().equalsIgnoreCase(text.trim())) return Optional.of(e);
        }
        return Optional.empty();
    }

    /* === 관리자: 이름/등급/역할 수정 === */
    @Transactional
    public void adminUpdateMemberBasic(Long mno, String memberName,
                                       Role role,
                                       Grade grade) {
        Member m = memberRepository.findById(mno)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("회원 없음: " + mno));

        if (memberName != null && !memberName.isBlank()) {
            m.setMemberName(memberName.trim());
        }
        if (role != null) {
            m.setRole(role);
        }
        if (grade != null) {
            m.setGrade(grade);
        }
    }

    /* === ✅ 관리자: 포인트 수정 === */
    @Transactional
    public void adminUpdateMemberPoints(Long mno, int points) {
        if (points < 0) throw new IllegalArgumentException("포인트는 0 이상이어야 합니다.");
        Member m = memberRepository.findById(mno)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("회원 없음: " + mno));
        m.setPoints(points);
    }

    /* === 관리자: 상태변경 또는 물리삭제(DROP) === */
    @Transactional
    public void adminDeleteOrWithdraw(Long mno, boolean drop, Status newStatus) {
        if (drop) {
            memberRepository.deleteById(mno);
            return;
        }
        Member m = memberRepository.findById(mno)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("회원 없음: " + mno));
        if (newStatus == null) throw new IllegalArgumentException("상태값이 필요합니다.");
        m.setStatus(newStatus);
    }
}