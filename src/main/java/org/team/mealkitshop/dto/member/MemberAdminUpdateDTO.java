package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MemberAdminUpdateDTO {

    @NotNull
    private Grade grade;

    @NotNull
    @Min(value = 0, message = "포인트는 0 이상이어야 합니다.")
    private Integer points;

   // 권한(ROLE_USER/ROLE_ADMIN 등)
    @NotNull
    private Role role;

    @NotNull
    private Status status;
}