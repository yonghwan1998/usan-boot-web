package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.repository.UserRepository;

import java.security.SecureRandom;

/**
 * @date    2026-07-02
 * @author  yongss
 * @desc    비밀번호 찾기 - 이메일/전화번호 일치 확인 및 임시 비밀번호 발급
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {String email, String phone}
     * @return  {boolean}
     *
     * 처리 과정:
     *  - 이메일과 전화번호가 동시에 일치하는 유저가 있는지 확인
     */
    public boolean existsByEmailAndPhone(String email, String phone) {
        return userRepository.findByEmailAndPhone(email, phone).isPresent();
    }

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {String phone}
     * @return  {boolean}
     *
     * 처리 과정:
     *  - 전화번호로 유저 조회 후 임시 비밀번호 생성
     *  - 유저 비밀번호를 임시 비밀번호로 변경 후 SMS로 발송
     */
    @Transactional
    public boolean resetPasswordAndNotify(String phone) {
        return userRepository.findByPhone(phone)
                .map(user -> {
                    String tempPassword = generateTempPassword();
                    user.setPasswordHash(passwordEncoder.encode(tempPassword));
                    userRepository.save(user);
                    smsService.sendTempPassword(phone, tempPassword);
                    return true;
                })
                .orElse(false);
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(secureRandom.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
