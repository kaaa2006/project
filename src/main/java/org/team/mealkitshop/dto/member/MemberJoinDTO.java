package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberJoinDTO {

    @NotBlank @Email
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    @Size(min = 8, max = 64)
    private String confirmPassword;

    @NotBlank
    @Size(max = 20)
    private String memberName;

    @NotBlank
    @Size(max = 20)
    private String phone;

    private boolean marketingYn;

    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordMatched() {
        return password != null && password.equals(confirmPassword);
    }

}
