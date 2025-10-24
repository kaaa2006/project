package org.team.mealkitshop.dto.member;

import lombok.*;
import org.team.mealkitshop.common.Grade;
import org.team.mealkitshop.common.Provider;
import org.team.mealkitshop.common.Role;
import org.team.mealkitshop.common.Status;
import org.team.mealkitshop.dto.address.AddressResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberAdminDetailDTO {
    private Long mno;
    private LocalDateTime createdAt;
    private String email;
    private String memberName;
    private String phone;
    private Grade grade;
    private Role role;
    private Integer points;
    private Provider provider; // 소셜로그인 사용자여부
    private Status status;
    private boolean marketingYn;

    private List<AddressResponseDTO> addresses;

}


