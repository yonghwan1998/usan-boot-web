package world.usan.usan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @date    2025-12-19
 * @author  yongss
 * @param   {String email, String password, String passwordConfirm, String phone, String nickname}
 *
 * 처리 과정:
 *  - 회원가입 요청 DTO
 */
public record JoinRequest(
        @NotBlank String email,

        @NotBlank
        @Size(min = 12, message = "비밀번호는 12자 이상")
        String password,

        @NotBlank String passwordConfirm,

        @NotBlank
        @Pattern(regexp = "^[0-9\\-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
        String phone,

        @NotBlank
        @Size(min = 2, max = 20, message = "닉네임은 2~20자")
        String nickname
) {
}
