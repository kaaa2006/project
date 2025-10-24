package org.team.mealkitshop.repository.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.team.mealkitshop.dto.member.MemberAdminListItemDTO;
import org.team.mealkitshop.dto.member.MemberSearchCondition;

public interface MemberRepositoryCustom {
    Page<MemberAdminListItemDTO> searchPage(MemberSearchCondition cond, Pageable pageable);

}
