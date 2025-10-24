package org.team.mealkitshop.dto.member;

import lombok.Getter;
import lombok.Setter;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;

@Getter
@Setter
public class MemberSearchCondition {
    private String keyword;      // email 또는 memberName 부분 검색
    private Grade grade;
    private Role role;
    private Provider provider;   // LOCAL / KAKAO ...
    private Status status;       // ACTIVE / WITHDRAWN
}
