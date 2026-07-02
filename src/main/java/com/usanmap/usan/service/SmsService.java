package com.usanmap.usan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.entity.ListingSendHistory;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;
import com.usanmap.usan.repository.ListingSendHistoryRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class SmsService {

    private final WebClient sensWebClient;
    private final BrokerPropertyCountRepository brokerRepository;
    private final ListingSendHistoryRepository listingSendHistoryRepository;
    private final CreditService creditService;

    @Value("${ncp.access-key}")
    private String accessKey;

    @Value("${ncp.secret-key}")
    private String secretKey;

    @Value("${ncp.sens.service-id}")
    private String serviceId;

    @Value("${ncp.sens.from}")
    private String fromNumber;

    @Value("${usan.base-url}")
    private String baseUrl;

    public SmsService(WebClient sensWebClient, BrokerPropertyCountRepository brokerRepository,
                      ListingSendHistoryRepository listingSendHistoryRepository, CreditService creditService) {
        this.sensWebClient = sensWebClient;
        this.brokerRepository = brokerRepository;
        this.listingSendHistoryRepository = listingSendHistoryRepository;
        this.creditService = creditService;
    }

    private static final String ADMIN_PHONE = "01033933402";

    public void sendBankTransferRequest(String userPhone, String depositorName, String productName, int amount, String bankInfo) {
        String userContent = "[우산] 입금 신청 완료\n"
                + "상품: " + productName + "\n"
                + "금액: " + String.format("%,d", amount) + "원\n"
                + "입금자명: " + depositorName + "\n"
                + "계좌: " + bankInfo + "\n"
                + "입금 확인 후 크레딧이 지급됩니다.";

        String adminContent = "[우산] 무통장 입금 신청\n"
                + "상품: " + productName + "\n"
                + "금액: " + String.format("%,d", amount) + "원\n"
                + "입금자명: " + depositorName + "\n"
                + "신청자: " + (userPhone != null ? userPhone : "번호없음");

        if (userPhone != null && !userPhone.isBlank()) {
            sendSms(List.of(userPhone.replaceAll("[^0-9]", ""), ADMIN_PHONE), userContent, adminContent);
        } else {
            sendSms(List.of(ADMIN_PHONE), adminContent, null);
        }
    }

    public void sendVerificationCode(String phone, String code) {
        String content = "[우산] 인증번호 [" + code + "]를 입력해 주세요.";
        sendSms(List.of(phone.replaceAll("[^0-9]", "")), content, null);
    }

    public void sendTempPassword(String phone, String tempPassword) {
        String content = "[우산] 임시 비밀번호는 [" + tempPassword + "] 입니다.\n로그인 후 반드시 비밀번호를 변경해 주세요.";
        sendSms(List.of(phone.replaceAll("[^0-9]", "")), content, null);
    }

    public void sendBankTransferApproved(String userPhone, int creditAmount) {
        if (userPhone == null || userPhone.isBlank()) {
            log.warn("승인 SMS 발송 실패 - 유저 전화번호 없음");
            return;
        }
        String content = "[우산] 크레딧 충전 완료!\n"
                + String.format("%,d", creditAmount) + "C이 충전되었습니다.\n"
                + "우리 동네 부동산에서 충전 여부를 확인해 주세요!";
        sendSms(List.of(userPhone.replaceAll("[^0-9]", "")), content, null);
    }

    private void sendSms(List<String> toNumbers, String defaultContent, String lastContent) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String url = "/sms/v2/services/" + serviceId + "/messages";
        String signature = makeSignature(timestamp, url);

        for (int i = 0; i < toNumbers.size(); i++) {
            String number = toNumbers.get(i);
            String content = (lastContent != null && i == toNumbers.size() - 1) ? lastContent : defaultContent;

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", "LMS");
            body.put("from", fromNumber);
            body.put("content", content);
            body.put("messages", List.of(Map.of("to", number)));

            try {
                sensWebClient.post()
                        .uri(url)
                        .header("x-ncp-apigw-timestamp", timestamp)
                        .header("x-ncp-iam-access-key", accessKey)
                        .header("x-ncp-apigw-signature-v2", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.info("SMS 발송 완료 - to: {}", number);
            } catch (Exception e) {
                log.error("SMS 발송 실패 - to: {}", number, e);
            }
        }
    }

    public void sendListingShare(List<UUID> brokerCodes, Listing listing, Long userId) {
        List<BrokerPropertyCount> brokers = brokerRepository.findByBrokerCodeIn(brokerCodes);

        List<BrokerPropertyCount> targets = brokers.stream()
                .filter(b -> b.getPhone() != null && !b.getPhone().isBlank())
                .toList();

        if (targets.isEmpty()) {
            log.warn("SMS 발송 대상 없음 - brokerCodes: {}", brokerCodes);
            return;
        }

        // TODO: 테스트용 - 모든 수신자를 01033933402로 고정 (롤백 시 아래 주석 해제 후 이 블록 제거)
        List<Map<String, String>> recipients = targets.stream()
                .map(b -> Map.of("to", "01033933402"))
                .toList();

        // [원본 코드]
        // List<Map<String, String>> recipients = targets.stream()
        //         .map(b -> Map.of("to", b.getPhone().replaceAll("[^0-9]", "")))
        //         .toList();

        // 크레딧 먼저 차감 - 부족 시 IllegalStateException 발생, 위로 전파
        creditService.deductForShare(userId, targets.size(), listing.getId());

        String content = buildMessage(listing);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String url = "/sms/v2/services/" + serviceId + "/messages";
        String signature = makeSignature(timestamp, url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "LMS");
        body.put("from", fromNumber);
        body.put("content", content);
        body.put("messages", recipients);

        try {
            sensWebClient.post()
                    .uri(url)
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", accessKey)
                    .header("x-ncp-apigw-signature-v2", signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS 발송 완료 - listingId: {}, 수신자: {}명", listing.getId(), targets.size());

            List<ListingSendHistory> histories = targets.stream()
                    .map(b -> ListingSendHistory.builder()
                            .listingId(listing.getId())
                            .userId(userId)
                            .brokerPhone(b.getPhone())
                            .build())
                    .toList();
            listingSendHistoryRepository.saveAll(histories);

        } catch (Exception e) {
            log.error("SMS 발송 실패 - listingId: {}", listing.getId(), e);
        }
    }

    private String buildMessage(Listing listing) {
        StringBuilder sb = new StringBuilder();

        if (listing.getTradeType().equals("SALE")) {
            sb.append("[우산] 매도합니다.\n");

            sb.append("@매물 정보\n");
            sb.append("주소: ").append(listing.getAddressName()).append("\n");
            sb.append("가격: ").append(listing.getPriceManwon()).append("만원\n");
            sb.append("설명: ").append(listing.getDescription()).append("\n\n");

            sb.append("@소유주 정보\n");
            sb.append("이름: ").append(listing.getOwnerName()).append("\n");
            sb.append("전화번호: ").append(listing.getOwnerPhone()).append("\n");
            sb.append("통신사: ").append(listing.getCarrier()).append("\n\n");

            sb.append("상세보기(매물 이미지, 상세 설명)\n");
            sb.append(baseUrl).append("/l/").append(listing.getPublicId());
        } else {
            sb.append("[우산] 임대합니다.\n");

            sb.append("@매물 정보\n");
            sb.append("주소: ").append(listing.getAddressName()).append("\n");
            sb.append("보증금: ").append(listing.getDepositManwon()).append("만원\n");
            sb.append("월세: ").append(listing.getRentManwon()).append("만원\n");
            sb.append("동·호수: ").append(listing.getDongho()).append("\n");
            sb.append("층수: ").append(listing.getFloorInfo()).append("\n");
            sb.append("면적(㎡): ").append(listing.getAreaM2()).append("\n");
            sb.append("설명: ").append(listing.getDescription()).append("\n\n");

            sb.append("@소유주 정보\n");
            sb.append("이름: ").append(listing.getOwnerName()).append("\n");
            sb.append("전화번호: ").append(listing.getOwnerPhone()).append("\n");
            sb.append("통신사: ").append(listing.getCarrier()).append("\n\n");

            sb.append("상세보기(매물 이미지, 상세 설명)\n");
            sb.append(baseUrl).append("/l/").append(listing.getPublicId());
        }

        return sb.toString().trim();
    }

    private String makeSignature(String timestamp, String url) {
        String message = "POST " + url + "\n" + timestamp + "\n" + accessKey;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("SMS 서명 생성 실패", e);
        }
    }
}
