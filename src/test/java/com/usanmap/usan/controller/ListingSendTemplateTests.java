package com.usanmap.usan.controller;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.service.ListingSendService.BatchView;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.mock.web.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ListingSendTemplateTests {
    private String render(String template, Map<String, Object> variables) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/"); resolver.setSuffix(".html"); resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servlet = new MockServletContext();
        var request = new MockHttpServletRequest(servlet);
        var exchange = JakartaServletWebApplication.buildApplication(servlet)
                .buildExchange(request, new MockHttpServletResponse());
        WebContext context = new WebContext(exchange, Locale.KOREAN);
        context.setVariable("showPcSideContent", false);
        context.setVariable("showHeader", false);
        context.setVariable("showSearchHeader", false);
        context.setVariable("showBottomNav", false);
        context.setVariable("currentUri", "/listings/send-history");
        context.setVariable("login", com.usanmap.usan.dto.LoginViewModel.anonymous());
        context.setVariable("listingLabels", Map.of(1L, "매물 1"));
        context.setVariable("filter", "ALL");
        context.setVariable("brokerAddresses", Map.of(1L, new com.usanmap.usan.dto.BrokerAddressDto("서울특별시 강남구 압구정로 123", "서울특별시 강남구 신사동 123-45")));
        context.setVariables(variables);
        String result = engine.process("pages/listings/" + template, context);
        // Used for isolated browser checks without a database or external SMS calls.
        try { java.nio.file.Files.createDirectories(java.nio.file.Path.of("build/send-history-preview"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("build/send-history-preview/" + template + (template.equals("send-history") && ((Page<?>)variables.get("batches")).isEmpty() ? ((Boolean)variables.get("hasLegacy") ? "-legacy-only" : "-empty") : "") + ".html"), result);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        return result;
    }

    private BatchView item() {
        LocalDateTime time = LocalDateTime.of(2026, 11, 20, 10, 30);
        return new BatchView(ListingSendBatch.builder().id(8L).listingId(1L).userId(1L)
                .listingTitle("우산갤러리아오피스텔 1201호").listingAddress("서울특별시 강남구 신사동 123-45")
                .listingCreatedAt(time).listingUpdatedAt(time).requestedAt(time)
                .messageType("LMS").requestStatus("ACCEPTED").recipientCount(7)
                .creditBefore(103).creditUsed(7).creditAfter(96).build(), 0, 0, 7);
    }

    @Test
    void rendersEmptyStateAndLegacyNavigationSeparately() {
        String empty = render("send-history", Map.of("batches", Page.empty(), "hasLegacy", false));
        assertThat(empty).contains("매물을 발송한 내역이 없어요").doesNotContain("이전 발송 내역 보기");
        String legacy = render("send-history", Map.of("batches", Page.empty(), "hasLegacy", true));
        assertThat(legacy).contains("이전 발송 내역 보기").doesNotContain("매물을 발송한 내역이 없어요");
    }

    @Test
    void rendersBatchSnapshotAndScopedRecipientLink() {
        String html = render("send-history", Map.of("batches", new PageImpl<>(List.of(item())), "hasLegacy", false));
        assertThat(html).contains("/listings/send-history/8", "수신중개사 보기", "매물 1", "등록일")
                .doesNotContain("send-history__request", "send-history__notice");
    }

    @Test
    void rendersDetailAndEscapesRecipientContent() {
        ListingSendHistory recipient = ListingSendHistory.builder().brokerOfficeName("<script>alert(1)</script>")
                .brokerPhone("01012345678").deliveryStatus("ACCEPTED").sentAt(LocalDateTime.now()).build();
        String html = render("send-history-detail", Map.of("item", item(), "recipients", new PageImpl<>(List.of(recipient))));
        assertThat(html).contains("전송 중", "&lt;script&gt;", "전송 완료", "환불", "data-address-trigger")
                .doesNotContain("<script>alert(1)</script>", "send-history__actions", "더 보기", "거래시각");
        render("send-history-legacy", Map.of("recipients", new PageImpl<>(List.of(recipient))));
        // 실제 사진 데이터가 없는 경우의 기본 이미지와 여러 상태를 시각 검증한다.
        List<ListingSendHistory> preview = List.of(
                recipient("우리우산부동산", "ACCEPTED", 0), recipient("가로수부동산", "DELIVERED", 0),
                recipient("신사프라임공인중개사사무소", "FAILED", 1), recipient("로얄강남부동산", "FAILED", 1),
                recipient("보헤미앙공인중개사", "FAILED", 1));
        for (String filter : List.of("DELIVERED", "SENDING", "REFUNDED")) {
            var filtered = preview.stream().filter(h -> switch (filter) {
                case "DELIVERED" -> "DELIVERED".equals(h.getDeliveryStatus()) && h.getRefundedCredit() == 0;
                case "SENDING" -> "ACCEPTED".equals(h.getDeliveryStatus()) && h.getRefundedCredit() == 0;
                default -> h.getRefundedCredit() > 0;
            }).toList();
            String filteredHtml = render("send-history-detail", Map.of("item", item(), "recipients", new PageImpl<>(filtered), "filter", filter));
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("build/send-history-preview/send-history-detail-" + filter + ".html"), filteredHtml); }
            catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        render("send-history-detail", Map.of("item", item(), "recipients", new PageImpl<>(preview)));
    }

    @Test
    void rendersVisitedRecipientAndBatchVisitRateWithoutExposingToken() {
        var recipient = recipient("우리우산부동산", "DELIVERED", 0);
        org.springframework.test.util.ReflectionTestUtils.setField(recipient, "trackingToken", "Ab3x_K");
        var visits = org.mockito.Mockito.mock(com.usanmap.usan.repository.ListingLinkVisitRepository.RecipientVisits.class);
        org.mockito.Mockito.when(visits.getVisitCount()).thenReturn(2L);
        org.mockito.Mockito.when(visits.getLastVisitedAt()).thenReturn(LocalDateTime.of(2026, 11, 27, 10, 30));
        var batch = org.mockito.Mockito.mock(com.usanmap.usan.repository.ListingLinkVisitRepository.BatchVisits.class);
        org.mockito.Mockito.when(batch.getTrackedRecipients()).thenReturn(2L);
        org.mockito.Mockito.when(batch.getVisitedRecipients()).thenReturn(1L);
        org.mockito.Mockito.when(batch.getVisitRate()).thenReturn(50.0);
        String html = render("send-history-detail", Map.of("item", item(), "recipients", new PageImpl<>(List.of(recipient)),
                "recipientVisits", Map.of(1L, visits), "batchVisits", Map.of(8L, batch)));
        String list = render("send-history", Map.of("batches", new PageImpl<>(List.of(item())),
                "hasLegacy", false, "batchVisits", Map.of(8L, batch)));
        assertThat(list).contains("send-history__visit-summary", "50.0%");
        assertThat(html).contains("send-history__visit-summary", "send-history__visit-badge", "링크 방문 2회", "2026-11-27 10:30", "50.0%")
                .doesNotContain("Ab3x_K");
    }

    @Test
    void hidesVisitSummaryAndEmptyRecipientVisitsWhenRateIsZero() {
        var batch = org.mockito.Mockito.mock(com.usanmap.usan.repository.ListingLinkVisitRepository.BatchVisits.class);
        var visits = Map.of(8L, batch);
        String list = render("send-history", Map.of("batches", new PageImpl<>(List.of(item())),
                "hasLegacy", false, "batchVisits", visits));
        String detail = render("send-history-detail", Map.of("item", item(),
                "recipients", new PageImpl<>(List.of(recipient("우리우산부동산", "DELIVERED", 0))),
                "batchVisits", visits, "recipientVisits", Map.of()));
        assertThat(list).doesNotContain("send-history__visit-summary", "0.0%");
        assertThat(detail).doesNotContain("send-history__visit-summary", "send-history__broker-visit",
                "방문 추적 대상 아님", "링크 방문 기록 없음", "0.0%");
    }

    private ListingSendHistory recipient(String name, String status, int refund) {
        return ListingSendHistory.builder().id(name.equals("우리우산부동산") ? 1L : (long)Math.abs(name.hashCode())).brokerOfficeName(name).brokerPhone("01012345678")
                .brokerAddress("서울특별시 강남구 신사동 123-45").deliveryStatus(status).refundedCredit(refund)
                .sentAt(LocalDateTime.of(2026, 11, 20, 10, 30)).build();
    }

    @Test
    void repeatedSendUsesSameMyListingLabelAndDeletedListingHasNoGlobalNumber() {
        ListingSendBatch second = ListingSendBatch.builder().id(9L).listingId(1L).userId(1L)
                .listingTitle("반복 발송").listingAddress("주소").recipientCount(1).requestStatus("ACCEPTED")
                .requestedAt(LocalDateTime.now()).build();
        String html = render("send-history", Map.of("batches", new PageImpl<>(List.of(item(), new BatchView(second, 0, 0, 1))),
                "hasLegacy", false, "listingLabels", Map.of(1L, "매물 2")));
        assertThat(html).containsPattern("send-history__tag[^>]*>매물 2");
        assertThat(html.split("매물 2", -1)).hasSize(3);
        String deleted = render("send-history", Map.of("batches", new PageImpl<>(List.of(item())),
                "hasLegacy", false, "listingLabels", Map.of()));
        assertThat(deleted).contains("삭제된 매물");
        render("send-history", Map.of("batches", new PageImpl<>(List.of(item())), "hasLegacy", false));
    }
}
