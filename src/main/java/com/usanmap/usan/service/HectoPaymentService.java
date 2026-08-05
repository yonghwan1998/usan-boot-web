package com.usanmap.usan.service;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.util.HectoCryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 헥토파이낸셜 신용카드 결제창(FORM 방식) 연동.
 * 참고: https://developers.hectofinancial.co.kr/docs/api/pg/credit-card/01-card-payment
 */
@Service
public class HectoPaymentService {

    private static final String METHOD_CARD = "CA";
    private static final String SUCCESS_STAT_CD = "0021";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TM_FMT = DateTimeFormatter.ofPattern("HHmmss");

    @Value("${hecto.mcht-id}")
    private String mchtId;

    @Value("${hecto.hash-key}")
    private String hashKey;

    @Value("${hecto.enc-key}")
    private String encKey;

    @Value("${hecto.mcht-name}")
    private String mchtName;

    @Value("${hecto.mcht-ename}")
    private String mchtEname;

    @Value("${hecto.test-mode:true}")
    private boolean testMode;

    @Value("${usan.base-url}")
    private String baseUrl;

    public String getPaymentActionUrl() {
        return testMode
                ? "https://tbnpg.settlebank.co.kr/card/main.do"
                : "https://npg.settlebank.co.kr/card/main.do";
    }

    /** 결제창(FORM-SUBMIT)에 그대로 실어 보낼 파라미터를 생성한다. 해시/암호화는 반드시 서버에서 처리. */
    public Map<String, String> buildPaymentParams(CreditOrder order) {
        if (mchtId == null || mchtId.isBlank()) {
            throw new IllegalStateException("헥토파이낸셜 상점ID(HECTO_MCHT_ID)가 설정되지 않았습니다. .env를 확인해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        String trdDt = now.format(DT_FMT);
        String trdTm = now.format(TM_FMT);
        String trdAmtPlain = String.valueOf(order.getPriceAmountSnapshot());
        String trdAmtEnc = HectoCryptoUtil.encryptAes256Ecb(trdAmtPlain, encKey);

        String hashSource = mchtId + METHOD_CARD + order.getOrderNo() + trdDt + trdTm + trdAmtPlain + hashKey;
        String pktHash = HectoCryptoUtil.sha256Hex(hashSource);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchtId", mchtId);
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
        return params;
    }

    /**
     * 노티(서버통보)/리턴(nextUrl) 응답의 위변조 여부를 검증한다.
     * 해시값 = SHA256(outStatCd + 거래일자 + 거래시간 + mchtId + mchtTrdNo + trdAmt + hashKey)
     */
    public boolean verifyResultHash(Map<String, String> result) {
        String trdDtm = result.get("trdDtm");
        String outStatCd = result.get("outStatCd");
        String mchtTrdNo = result.get("mchtTrdNo");
        String trdAmt = result.get("trdAmt");
        String pktHash = result.get("pktHash");

        if (trdDtm == null || trdDtm.length() != 14 || outStatCd == null
                || mchtTrdNo == null || trdAmt == null || pktHash == null) {
            return false;
        }

        String trdDt = trdDtm.substring(0, 8);
        String trdTm = trdDtm.substring(8, 14);
        String expected = HectoCryptoUtil.sha256Hex(outStatCd + trdDt + trdTm + mchtId + mchtTrdNo + trdAmt + hashKey);
        return expected.equals(pktHash);
    }

    public boolean isSuccess(Map<String, String> result) {
        return SUCCESS_STAT_CD.equals(result.get("outStatCd"));
    }
}
