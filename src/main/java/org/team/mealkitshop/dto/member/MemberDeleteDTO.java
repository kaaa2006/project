package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDeleteDTO {
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
    private String currentPassword; // Provider.Local일 때만 필수
    private String reason;          // 선택
    private String memo;
}
