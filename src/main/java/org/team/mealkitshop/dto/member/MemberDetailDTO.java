// 회원 상세 + 주소 목록
package org.team.mealkitshop.dto.member;

import lombok.*;
import org.team.mealkitshop.common.Grade;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MemberDetailDTO {
    private Long mno;
    private String email;
    private String memberName;
    private String phone;
    private Grade grade;
    private Integer points;
    private Boolean marketingYn;

    @Builder.Default
    private List<AddressDTO> addresses = Collections.emptyList();

    @Getter
    @Setter
    public static class AddressDTO {
        private Long id;
        private String alias;
        private String zipCode;
        private String addr1;
        private String addr2;
        private Boolean isDefault;

    }
}