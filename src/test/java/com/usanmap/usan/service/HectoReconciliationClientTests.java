package com.usanmap.usan.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HectoReconciliationClientTests {

    private final HectoReconciliationClient client = new HectoReconciliationClient(WebClient.builder());

    @Test
    void parsesOfficialPipeDelimitedResponse() {
        String body = "0000|정상 처리 되었습니다.|2|\n" +
                "test-merchant|CA|TRD-APPROVAL|ORD-123|0|20261030|100000|1000|||홍길동|테스트상품|99100001|02|00|N|\n" +
                "test-merchant|CA|TRD-CANCEL|ORD-123|1|20261030|110000|1000|TRD-APPROVAL|20261030|홍길동|테스트상품|99100001|02|00|N|\n";

        HectoReconciliationResponse response = client.parse(body);

        assertThat(response.resultCode()).isEqualTo("0000");
        assertThat(response.transactions()).hasSize(2);
        assertThat(response.transactions().get(0).type())
                .isEqualTo(HectoReconciliationTransaction.Type.APPROVAL);
        assertThat(response.transactions().get(1).type())
                .isEqualTo(HectoReconciliationTransaction.Type.CANCELLATION);
        assertThat(response.transactions().get(1).originalTransactionNo()).isEqualTo("TRD-APPROVAL");
    }

    @Test
    void treatsNoTransactionsResultAsEmptyResponse() {
        HectoReconciliationResponse response = client.parse("1001|조회내역 없음|0|\n");

        assertThat(response.transactions()).isEmpty();
    }

    @Test
    void rejectsTruncatedResponseInsteadOfPartiallyRecovering() {
        String body = "0000|정상 처리 되었습니다.|2|\n" +
                "test-merchant|CA|TRD-1|ORD-1|0|20261030|100000|1000|\n";

        assertThatThrownBy(() -> client.parse(body))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("건수가 일치하지 않습니다");
    }

    @Test
    void rejectsPgErrorResponse() {
        assertThatThrownBy(() -> client.parse("ST92|해쉬 값 불일치.|0|\n"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ST92");
    }
}
