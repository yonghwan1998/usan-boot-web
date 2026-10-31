package com.usanmap.usan.service;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.util.HectoCryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 결제창(FORM 방식) 연동.
 * 참고: https://developers.hectofinancial.co.kr/docs/api/pg/credit-card/01-card-payment
 */
@Service
@RequiredArgsConstructor
public class HectoPaymentService {

    private static final String METHOD_CARD = "card";
    private static final String SUCCESS_STAT_CD = "0021";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TM_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private final HectoPaymentProfiles paymentProfiles;

    @Value("${hecto.mcht-name}")
    private String mchtName;

    @Value("${hecto.mcht-ename}")
    private String mchtEname;

    @Value("${usan.base-url}")
    private String baseUrl;

    /** 결제창(FORM-SUBMIT)에 그대로 실어 보낼 파라미터를 생성한다. 해시/암호화는 반드시 서버에서 처리. */
    public PaymentRequest buildPaymentRequest(CreditOrder order, String accountEmail) {
        HectoPaymentProfiles.PaymentProfile profile = paymentProfiles.forAccount(accountEmail);
        if (!profile.isConfigured()) {
            throw new IllegalStateException("헥토파이낸셜 " + profile.environment().name().toLowerCase()
                    + " 결제 설정을 확인해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        String trdDt = now.format(DT_FMT);
        String trdTm = now.format(TM_FMT);
        String trdAmtPlain = String.valueOf(order.getPriceAmountSnapshot());
        String trdAmtEnc = HectoCryptoUtil.encryptAes256Ecb(trdAmtPlain, profile.encKey());

        String hashSource = profile.mchtId() + METHOD_CARD + order.getOrderNo()
                + trdDt + trdTm + trdAmtPlain + profile.hashKey();
        String pktHash = HectoCryptoUtil.sha256Hex(hashSource);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchtId", profile.mchtId());
        params.put("method", METHOD_CARD);
        params.put("trdDt", trdDt);
        params.put("trdTm", trdTm);
        params.put("mchtTrdNo", order.getOrderNo());
        params.put("mchtName", mchtName);
        params.put("mchtEName", mchtEname);
        params.put("pmtPrdtNm", order.getProductNameSnapshot());
        params.put("trdAmt", trdAmtEnc);
        params.put("notiUrl", baseUrl + "/credits/hecto/noti");
        params.put("nextUrl", baseUrl + "/credits/hecto/return");
        params.put("cancUrl", baseUrl + "/credits/hecto/cancel");
        params.put("pktHash", pktHash);
        return new PaymentRequest(profile.environment(), profile.actionUrl(), params);
    }

    /**
     * 노티(서버통보) 응답의 위변조 여부를 검증한다.
     * 해시값 = SHA256(outStatCd + 거래일자 + 거래시간 + mchtId + mchtTrdNo + trdAmt + hashKey)
     */
    public boolean verifyNotificationHash(Map<String, String> result) {
        String trdDtm = result.get("trdDtm");
        String outStatCd = result.get("outStatCd");
        String mchtTrdNo = result.get("mchtTrdNo");
        String trdAmt = result.get("trdAmt");
        String pktHash = result.get("pktHash");
        String receivedMchtId = result.get("mchtId");
        String transactionNo = result.get("trdNo");

        HectoPaymentProfiles.PaymentProfile profile = paymentProfiles.findByMerchantId(receivedMchtId).orElse(null);
        if (profile == null || trdDtm == null || trdDtm.length() != 14 || outStatCd == null
                || mchtTrdNo == null || trdAmt == null || pktHash == null
                || transactionNo == null || transactionNo.isBlank()) {
            return false;
        }

        String trdDt = trdDtm.substring(0, 8);
        String trdTm = trdDtm.substring(8, 14);
        String expected = HectoCryptoUtil.sha256Hex(outStatCd + trdDt + trdTm
                + receivedMchtId + mchtTrdNo + trdAmt + profile.hashKey());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                pktHash.getBytes(StandardCharsets.US_ASCII)
        );
    }

    public boolean isSuccess(Map<String, String> result) {
        return SUCCESS_STAT_CD.equals(result.get("outStatCd"));
    }

    public record PaymentRequest(
            HectoPaymentProfiles.Environment environment,
            String actionUrl,
            Map<String, String> params
    ) {}
}
