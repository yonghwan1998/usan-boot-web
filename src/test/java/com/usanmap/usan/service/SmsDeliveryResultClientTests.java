package com.usanmap.usan.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsDeliveryResultClientTests {
    private SmsService service(ExchangeFunction exchange) {
        SmsService sms = new SmsService(WebClient.builder().baseUrl("https://sens.apigw.ntruss.com")
                .exchangeFunction(exchange).build(), mock(com.usanmap.usan.repository.BrokerPropertyCountRepository.class),
                mock(ListingSendService.class));
        ReflectionTestUtils.setField(sms, "secretKey", "test-secret");
        ReflectionTestUtils.setField(sms, "accessKey", "test-key");
        ReflectionTestUtils.setField(sms, "serviceId", "ncp:sms:kr:test");
        return sms;
    }

    @Test
    void fetchesAllPagesAndSignsTheExactEncodedGetUri() throws Exception {
        List<ClientRequest> requests = new ArrayList<>();
        SmsService sms = service(request -> {
            requests.add(request);
            return Mono.just(response(requests.size() == 1 ? """
                    {"statusCode":"202","statusName":"success","messages":[
                     {"requestId":"request-1","messageId":"m1","to":"01012345678","status":"PROCESSING"}],
                     "hasMore":true,"nextToken":"a+b/="}
                    """ : """
                    {"statusCode":"202","statusName":"success","messages":[
                     {"requestId":"request-1","messageId":"m2","to":"01022223333","status":"COMPLETED",
                      "statusName":"success","statusCode":"0","completeTime":"2026-11-20 10:01:00"}],"hasMore":false}
                    """));
        });
        var results = sms.getListingSendResults("request-1");
        assertThat(results).hasSize(2);
        assertThat(results.get(1).completeTime()).isEqualTo(java.time.LocalDateTime.of(2026,11,20,10,1));
        assertThat(requests.get(1).url().getRawQuery()).contains("nextToken=a%2Bb%2F%3D");
        for (ClientRequest request : requests) {
            assertThat(request.method()).isEqualTo(HttpMethod.GET);
            String signed = "GET " + request.url().getRawPath() + "?" + request.url().getRawQuery() + "\n"
                    + request.headers().getFirst("x-ncp-apigw-timestamp") + "\ntest-key";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            assertThat(request.headers().getFirst("x-ncp-apigw-signature-v2"))
                    .isEqualTo(Base64.getEncoder().encodeToString(mac.doFinal(signed.getBytes(StandardCharsets.UTF_8))));
        }
    }

    @Test
    void rejectsBrokenPaginationRatherThanApplyingPartialResults() {
        SmsService sms = service(request -> Mono.just(response("""
                {"statusCode":"202","statusName":"success","messages":[],"hasMore":true}
                """)));
        assertThatThrownBy(() -> sms.getListingSendResults("request-1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsUnsuccessfulApiResponse() {
        SmsService sms = service(request -> Mono.just(response("{\"statusCode\":\"500\",\"messages\":[]}")));
        assertThatThrownBy(() -> sms.getListingSendResults("request-1")).isInstanceOf(IllegalStateException.class);
    }

    private ClientResponse response(String json) {
        return ClientResponse.create(HttpStatus.ACCEPTED).header("Content-Type", "application/json").body(json).build();
    }
}
