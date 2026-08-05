package com.usanmap.usan.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 헥토파이낸셜 PG 연동용 해시/암호화 유틸.
 * 참고: https://developers.hectofinancial.co.kr/docs/api/pg
 */
public final class HectoCryptoUtil {

    private HectoCryptoUtil() {
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("해시 생성에 실패했습니다.", e);
        }
    }

    /**
     * AES-256/ECB/PKCS5Padding + Base64. 거래금액(trdAmt), 취소금액(cnclAmt) 암호화에 사용.
     * encKey는 반드시 32byte(AES-256) 문자열이어야 한다.
     */
    public static String encryptAes256Ecb(String plainText, String encKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("금액 암호화에 실패했습니다.", e);
        }
    }
}
