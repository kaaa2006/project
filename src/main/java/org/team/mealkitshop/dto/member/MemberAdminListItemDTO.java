package org.team.mealkitshop.dto.member;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberAdminListItemDTO {
    private LocalDateTime createdAt;
    private String email;
    private String memberName;
    private Grade grade;
    private Role role;
    private int points;
    private Provider provider;
    private Status status;
}