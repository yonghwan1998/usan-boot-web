package com.usanmap.usan.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.usanmap.usan.repository.UserRepository;
import com.usanmap.usan.service.PhoneVerificationService;

import java.util.Map;

@RequestMapping("/login")
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final PhoneVerificationService phoneVerificationService;
    private final UserRepository userRepository;

    @GetMapping("")
    public String loginPage() {
        return "pages/login/login";
    }

    @GetMapping("/find-id")
    public String findIdPage() {
        return "pages/login/find-id";
    }

    @GetMapping("/find-pw")
    public String findPwPage() {
        return "pages/login/find-pw";
    }

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {String phone, HttpSession session}
     *
     * 처리 과정:
     *  - 전달받은 전화번호로 6자리 인증번호 발송
     */
    @PostMapping("/api/send-code")
    @ResponseBody
    public Map<String, Object> sendCode(@RequestParam String phone, HttpSession session) {
        String normalizedPhone = phone.replaceAll("[^0-9]", "");

        phoneVerificationService.sendCode(session, normalizedPhone);
        return Map.of("success", true);
    }

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {String phone, String code, HttpSession session}
     *
     * 처리 과정:
     *  - 인증번호 검증 후 전화번호로 가입된 이메일 조회
     *  - 인증번호가 틀리면 INVALID_CODE, 이메일이 없으면(소셜 로그인 포함) NOT_FOUND, 있으면 SUCCESS 반환
     */
    @PostMapping("/api/find-id")
    @ResponseBody
    public Map<String, Object> findId(@RequestParam String phone, @RequestParam String code, HttpSession session) {
        String normalizedPhone = phone.replaceAll("[^0-9]", "");

        if (!phoneVerificationService.verifyCode(session, normalizedPhone, code)) {
            return Map.of("result", "INVALID_CODE");
        }

        return userRepository.findByPhone(normalizedPhone)
                .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                .<Map<String, Object>>map(user -> Map.of("result", "SUCCESS", "email", user.getEmail()))
                .orElse(Map.of("result", "NOT_FOUND"));
    }
}
