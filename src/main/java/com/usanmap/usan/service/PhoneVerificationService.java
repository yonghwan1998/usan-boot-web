package com.usanmap.usan.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * @date    2026-07-02
 * @author  yongss
 * @desc    전화번호 인증번호 발송/검증 (세션 기반)
 */
@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final String SESSION_PHONE = "PHONE_VERIFY_PHONE";
    private static final String SESSION_CODE = "PHONE_VERIFY_CODE";
    private static final String SESSION_EXPIRE_AT = "PHONE_VERIFY_EXPIRE_AT";
    private static final long CODE_TTL_MILLIS = 5 * 60 * 1000;

    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {HttpSession session, String phone}
     *
     * 처리 과정:
     *  - 6자리 인증번호 생성 후 세션에 저장
     *  - SMS로 인증번호 발송
     */
    public void sendCode(HttpSession session, String phone) {
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        session.setAttribute(SESSION_PHONE, phone);
        session.setAttribute(SESSION_CODE, code);
        session.setAttribute(SESSION_EXPIRE_AT, System.currentTimeMillis() + CODE_TTL_MILLIS);

        smsService.sendVerificationCode(phone, code);
    }

    /**
     * @date    2026-07-02
     * @author  yongss
     * @param   {HttpSession session, String phone, String code}
     * @return  {boolean}
     *
     * 처리 과정:
     *  - 세션에 저장된 전화번호/인증번호/만료시각과 비교하여 유효성 검증
     *  - 검증 성공 시 재사용 방지를 위해 세션 값 제거
     */
    public boolean verifyCode(HttpSession session, String phone, String code) {
        Object savedPhone = session.getAttribute(SESSION_PHONE);
        Object savedCode = session.getAttribute(SESSION_CODE);
        Object expireAt = session.getAttribute(SESSION_EXPIRE_AT);

        if (savedPhone == null || savedCode == null || expireAt == null) return false;
        if (System.currentTimeMillis() > (long) expireAt) return false;
        if (!savedPhone.equals(phone) || !savedCode.equals(code)) return false;

        session.removeAttribute(SESSION_PHONE);
        session.removeAttribute(SESSION_CODE);
        session.removeAttribute(SESSION_EXPIRE_AT);
        return true;
    }
}
