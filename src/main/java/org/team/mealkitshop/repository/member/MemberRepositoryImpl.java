package org.team.mealkitshop.repository.member;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.member.QMember;
import org.team.mealkitshop.dto.member.MemberAdminListItemDTO;
import org.team.mealkitshop.dto.member.MemberSearchCondition;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory query;
    private static final QMember m = QMember.member;

    @Override
    public Page<MemberAdminListItemDTO> searchPage(MemberSearchCondition cond, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        if (cond.getKeyword() != null && !cond.getKeyword().isBlank()) {
            String kw = "%" + cond.getKeyword().trim() + "%";
            where.and(m.email.likeIgnoreCase(kw).or(m.memberName.likeIgnoreCase(kw)));
        }
        if (cond.getGrade() != null)    where.and(m.grade.eq(cond.getGrade()));
        if (cond.getRole() != null)     where.and(m.role.eq(cond.getRole()));
        if (cond.getProvider() != null) where.and(m.provider.eq(cond.getProvider()));
        if (cond.getStatus() != null)   where.and(m.status.eq(cond.getStatus()));

        // 정렬: 기본 createdAt(=regTime) DESC
        List<OrderSpecifier<?>> orders = new ArrayList<>();
        if (pageable.getSort().isUnsorted()) {
            orders.add(new OrderSpecifier<>(Order.DESC, m.regTime)); // BaseTimeEntity 필드명에 맞춰 수정
        } else {
            for (Sort.Order o : pageable.getSort()) {
                Order dir = o.isAscending() ? Order.ASC : Order.DESC;
                switch (o.getProperty()) {
                    case "createdAt" -> orders.add(new OrderSpecifier<>(dir, m.regTime));
                    case "email" -> orders.add(new OrderSpecifier<>(dir, m.email));
                    case "memberName" -> orders.add(new OrderSpecifier<>(dir, m.memberName));
                    case "grade" -> orders.add(new OrderSpecifier<>(dir, m.grade));
                    case "role" -> orders.add(new OrderSpecifier<>(dir, m.role));
                    case "points" -> orders.add(new OrderSpecifier<>(dir, m.points));
                    case "provider" -> orders.add(new OrderSpecifier<>(dir, m.provider));
                    case "status" -> orders.add(new OrderSpecifier<>(dir, m.status));
                    default -> orders.add(new OrderSpecifier<>(Order.DESC, m.regTime));
                }
            }
        }

        List<MemberAdminListItemDTO> content = query
                .select(Projections.constructor(
                        MemberAdminListItemDTO.class,
                        m.regTime,        // createdAt
                        m.email,
                        m.memberName,
                        m.grade,
                        m.role,
                        m.points,
                        m.provider,
                        m.status
                ))
                .from(m)
                .where(where)
                .orderBy(orders.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = query.select(m.count()).from(m).where(where).fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
