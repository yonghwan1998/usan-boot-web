package com.usanmap.usan.service;

import com.usanmap.usan.entity.User;
import com.usanmap.usan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @date    2026-07-05
 * @author  yongss
 * @desc    로그인 유저의 비밀번호 변경 (마이페이지 - 비밀번호 재설정)
 */
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public enum Result { SUCCESS, WRONG_CURRENT_PASSWORD, SOCIAL_ACCOUNT, NOT_FOUND }

    /**
     * @date    2026-07-05
     * @author  yongss
     * @param   {Long userId, String currentPassword, String newPassword}
     * @return  {Result}
     *
     * 처리 과정:
     *  - 유저 조회 후 password_hash가 없으면(소셜 로그인 전용 계정) SOCIAL_ACCOUNT 반환
     *  - 현재 비밀번호 일치 여부 확인 후 일치하면 새 비밀번호로 암호화하여 저장
     */
    @Transactional
    public Result changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Result.NOT_FOUND;
        }

        if (user.getPasswordHash() == null) {
            return Result.SOCIAL_ACCOUNT;
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return Result.WRONG_CURRENT_PASSWORD;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.SUCCESS;
    }
}
