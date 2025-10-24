package org.team.mealkitshop.repository.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.team.mealkitshop.domain.member.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, QuerydslPredicateExecutor<Member> {

    // email 중복 체크
    boolean existsByEmail(String email);

    // email로 회원 조회
    Optional<Member> findByEmail(String email);

    // 닉네임(=memberName) 중복 체크
    boolean existsByMemberName(String memberName);

    Optional<Member> findByMemberName(String memberName);
}

