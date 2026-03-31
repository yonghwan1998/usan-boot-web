package com.usanmap.usan.dto;

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

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "이메일 형식을 확인해 주세요."
        )
        @NotBlank String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, message = "영문, 숫자, 특수문자를 조합해 8자 이상 적어주세요.")
        @Pattern(
                // 영문 + 숫자 + 특수문자 포함 (8자 이상)
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "영문, 숫자, 특수문자를 조합해 8자 이상 적어주세요."
        )
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
        String passwordConfirm,

        @NotBlank(message = "전화번호를 입력해 주세요.")
        @Pattern(
                // 010-1234-5678 / 01012345678 / 010 1234 5678 등 허용하려면 서버에서 정규화 후 검사하는 게 더 안전함
                regexp = "^(01[016789])[- ]?\\d{3,4}[- ]?\\d{4}$",
                message = "전화번호 형식을 확인해 주세요. 예) 010-1234-5678"
        )
        String phone,

        @Size(min = 2, max = 20, message = "닉네임은 2~20자")
        String nickname
) {
}
