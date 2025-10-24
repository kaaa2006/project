package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@ToString(exclude = {"password", "confirmPassword"})
public class SocialJoinDTO {

    @NotBlank
    @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;

    @NotBlank
    @Size(min = 8, max = 64)
    private String confirmPassword;

    @NotBlank
    @Size(max = 30)
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
