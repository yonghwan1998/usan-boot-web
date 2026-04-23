package com.usanmap.usan.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class SmsService {

    private final WebClient sensWebClient;
    private final BrokerPropertyCountRepository brokerRepository;

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

    public SmsService(WebClient sensWebClient, BrokerPropertyCountRepository brokerRepository) {
        this.sensWebClient = sensWebClient;
        this.brokerRepository = brokerRepository;
    }

    public void sendListingShare(List<UUID> brokerCodes, Listing listing) {
        List<BrokerPropertyCount> brokers = brokerRepository.findByBrokerCodeIn(brokerCodes);

        // TODO: 테스트용 - 모든 수신자를 01033933402로 고정 (롤백 시 아래 주석 해제 후 이 블록 제거)
        List<Map<String, String>> recipients = brokers.stream()
                .map(BrokerPropertyCount::getPhone)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> Map.of("to", "01033933402"))
                .toList();

        // [원본 코드]
        // List<Map<String, String>> recipients = brokers.stream()
        //         .map(BrokerPropertyCount::getPhone)
        //         .filter(p -> p != null && !p.isBlank())
        //         .map(p -> Map.of("to", p.replaceAll("[^0-9]", "")))
        //         .toList();

        if (recipients.isEmpty()) {
            log.warn("SMS 발송 대상 없음 - brokerCodes: {}", brokerCodes);
            return;
        }

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

            log.info("SMS 발송 완료 - listingId: {}, 수신자: {}명", listing.getId(), recipients.size());
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
