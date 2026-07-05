package com.usanmap.usan.controller;

import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.PasswordChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class InfoApiController {

    private final PasswordChangeService passwordChangeService;
    private final SecurityUtils securityUtils;

    /**
     * @date    2026-07-05
     * @author  yongss
     * @param   {String currentPassword, String newPassword}
     *
     * 처리 과정:
     *  - 로그인 유저의 현재 비밀번호 확인 후 새 비밀번호로 변경
     */
    @PostMapping("/change-pw")
    public Map<String, Object> changePassword(@RequestParam String currentPassword, @RequestParam String newPassword) {
        Long userId = securityUtils.currentUserIdOrThrow();
        PasswordChangeService.Result result = passwordChangeService.changePassword(userId, currentPassword, newPassword);
        return Map.of("result", result.name());
    }
}
