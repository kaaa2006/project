package org.team.mealkitshop.dto.member;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"currentPassword", "newPassword", "newPasswordConfirm"})
public class MemberUpdateDTO {

        // 전화번호 (선택): 비워도 되고, 입력하면 숫자/하이픈 1~20자
        @Pattern(regexp = "^$|[0-9\\-]{1,20}$", message = "휴대폰은 숫자/하이픈만 1~20자로 입력하세요.")
        private String phone;

        // 마케팅 수신 동의
        // primitive boolean이면 항상 true/false가 들어옵니다(Thymeleaf th:field는 hidden으로 false도 보냄).
        // 필요시 Boolean으로 바꿔 '미전송=null(미변경)'도 가능.
        private boolean marketingYn;

        // 비밀번호 변경 블록(선택): 비우면 통과, 입력하면 8~64자
        @Pattern(regexp = "^$|.{8,64}$", message = "현재 비밀번호는 8~64자여야 합니다.")
        private String currentPassword;

        @Pattern(regexp = "^$|.{8,64}$", message = "새 비밀번호는 8~64자여야 합니다.")
        private String newPassword;

        @Pattern(regexp = "^$|.{8,64}$", message = "새 비밀번호 확인은 8~64자여야 합니다.")
        private String newPasswordConfirm;

        /**
         * 비번 블록 검증:
         * - 셋 다 비었으면 통과 (비번 변경 안 함)
         * - 하나라도 입력했으면 셋 다 입력 & 새=확인 일치해야 통과
         */
        @AssertTrue(message = "비밀번호 변경을 하려면 현재/새/확인 비밀번호를 모두 입력하고 일치해야 합니다.")
        public boolean isPasswordBlockValid() {
                boolean any =
                        notBlank(currentPassword) || notBlank(newPassword) || notBlank(newPasswordConfirm);
                if (!any) return true; // 비번 변경 안 함 → 통과
                return notBlank(currentPassword)
                        && notBlank(newPassword)
                        && notBlank(newPasswordConfirm)
                        && newPassword.equals(newPasswordConfirm);
        }

        private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
