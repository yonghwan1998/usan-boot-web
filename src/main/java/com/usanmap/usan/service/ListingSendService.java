package com.usanmap.usan.service;

import com.usanmap.usan.entity.*;
import com.usanmap.usan.dto.SmsDeliveryResult;
import com.usanmap.usan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingSendService {
    private final ListingSendBatchRepository batchRepository;
    private final ListingSendHistoryRepository historyRepository;
    private final CreditService creditService;
    private final ListingService listingService;

    // 크레딧 원장, 발송 묶음, 수신자 이력을 함께 커밋한 후 외부 API를 호출한다.
    @Transactional
    public Long prepare(Listing listing, Long userId, List<BrokerPropertyCount> targets) {
        if (!Objects.equals(listing.getUserId(), userId)) {
            throw new IllegalStateException("발송 권한이 없습니다.");
        }
        if (targets.isEmpty()) throw new IllegalArgumentException("발송 대상이 없습니다.");
        CreditLedger ledger = creditService.deductForShare(userId, targets.size(), listing.getId());
        LocalDateTime now = LocalDateTime.now();
        String title = listing.getAddressDetail();
        if (title == null || title.isBlank()) title = listing.getAddressName();
        ListingSendBatch batch = batchRepository.save(ListingSendBatch.builder()
                .listingId(listing.getId()).userId(userId)
                .listingTitle(title).listingAddress(listing.getAddressName())
                .listingCreatedAt(listing.getCreatedAt()).listingUpdatedAt(listing.getUpdatedAt())
                .messageType("LMS").recipientCount(targets.size()).requestStatus("REQUESTED")
                .creditLedgerId(ledger.getId()).creditUsed(-ledger.getChangeAmount())
                .creditBefore(ledger.getBalanceAfter() - ledger.getChangeAmount())
                .creditAfter(ledger.getBalanceAfter())
                .requestedAt(now).updatedAt(now).build());
        historyRepository.saveAll(targets.stream().map(b -> ListingSendHistory.builder()
                .batchId(batch.getId()).listingId(listing.getId()).userId(userId)
                .brokerCode(b.getBrokerCode()).brokerPhone(b.getPhone())
                .brokerOfficeName(b.getOfficeName())
                .brokerAddress(b.getAddrRoad() != null && !b.getAddrRoad().isBlank() ? b.getAddrRoad() : b.getAddrJibun())
                .deliveryStatus("PENDING").refundedCredit(0).sentAt(now).build()).toList());
        return batch.getId();
    }

    @Transactional
    public void recordRequest(Long batchId, String status, String requestId, String resultCode) {
        ListingSendBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("발송 내역을 찾을 수 없습니다."));
        batch.markRequest(status, requestId);
        historyRepository.findAllByBatchIdOrderByIdAsc(batchId)
                .forEach(h -> h.markRequest(status, resultCode));
    }

    @Transactional
    public int applyDeliveryResults(Long batchId, String requestId, List<SmsDeliveryResult> results) {
        ListingSendBatch batch = batchRepository.findLockedById(batchId).orElse(null);
        if (batch == null || !Objects.equals(batch.getProviderRequestId(), requestId)) return 0;
        List<ListingSendHistory> histories = historyRepository.findAllByBatchIdOrderByIdAsc(batchId).stream()
                .filter(h -> Objects.equals(h.getUserId(), batch.getUserId())).toList();
        Map<String, SmsDeliveryResult> byMessageId = new LinkedHashMap<>();
        for (SmsDeliveryResult result : results) {
            if (Objects.equals(requestId, result.requestId()) && result.messageId() != null
                    && !result.messageId().isBlank() && result.messageId().length() <= 100) {
                byMessageId.put(result.messageId(), result);
            }
        }
        Map<String, List<SmsDeliveryResult>> byPhone = byMessageId.values().stream()
                .collect(Collectors.groupingBy(result -> normalizePhone(result.to())));
        Map<String, List<ListingSendHistory>> historiesByPhone = histories.stream()
                .collect(Collectors.groupingBy(h -> normalizePhone(h.getBrokerPhone())));
        Set<String> assignedMessageIds = histories.stream().map(ListingSendHistory::getProviderMessageId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        int updated = 0;
        for (ListingSendHistory history : histories) {
            if (!history.isDeliveryPending()) continue;
            String phone = normalizePhone(history.getBrokerPhone());
            if (phone.isEmpty()) continue;
            SmsDeliveryResult result = byMessageId.get(history.getProviderMessageId());
            if (result == null && history.getProviderMessageId() == null) {
                var candidates = byPhone.getOrDefault(phone, List.of());
                // 번호 중복이나 테스트 번호 대체 발송은 순서만으로 추정해 연결하지 않는다.
                if (historiesByPhone.get(phone).size() == 1 && candidates.size() == 1
                        && !assignedMessageIds.contains(candidates.get(0).messageId())) {
                    result = candidates.get(0);
                }
            }
            if (result == null || !phone.equals(normalizePhone(result.to()))
                    || !"COMPLETED".equals(result.status())
                    || !("success".equals(result.statusName()) || "fail".equals(result.statusName()))
                    || (result.statusCode() != null && result.statusCode().length() > 100)) continue;
            history.applyDeliveryResult(result.messageId(), "success".equals(result.statusName()),
                    result.statusCode(), result.completeTime());
            assignedMessageIds.add(result.messageId());
            updated++;
        }
        return updated;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.startsWith("82") && digits.length() == 12 ? "0" + digits.substring(2) : digits;
    }

    @Transactional(readOnly = true)
    public Page<BatchView> getBatches(Long userId, int page) {
        Page<ListingSendBatch> batches = batchRepository.findAllByUserIdOrderByRequestedAtDescIdDesc(
                userId, PageRequest.of(Math.max(0, page), 20));
        Map<Long, ListingSendHistoryRepository.BatchCounts> counts = batches.isEmpty() ? Map.of()
                : historyRepository.countByBatches(userId, batches.stream().map(ListingSendBatch::getId).toList())
                .stream().collect(Collectors.toMap(ListingSendHistoryRepository.BatchCounts::getBatchId, Function.identity()));
        return batches.map(b -> view(b, counts.get(b.getId())));
    }

    @Transactional(readOnly = true)
    public BatchView getBatch(Long userId, Long id) {
        ListingSendBatch batch = batchRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "발송 내역을 찾을 수 없습니다."));
        var counts = historyRepository.countByBatches(userId, List.of(id));
        return view(batch, counts.isEmpty() ? null : counts.get(0));
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getListingLabels(Long userId) {
        // /listings와 동일한 내 활성 매물 목록 및 정렬을 재사용한다.
        var listings = listingService.getMyListings(userId);
        Map<Long, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < listings.size(); i++) {
            labels.put(listings.get(i).listingId(), "매물 " + (i + 1));
        }
        return labels;
    }

    @Transactional(readOnly = true)
    public Page<ListingSendHistory> getRecipients(Long userId, Long batchId, int page, String filter) {
        if (!Set.of("ALL", "DELIVERED", "SENDING", "REFUNDED").contains(filter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 발송 상태입니다.");
        }
        if (batchRepository.findByIdAndUserId(batchId, userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "발송 내역을 찾을 수 없습니다.");
        }
        return historyRepository.findRecipients(batchId, userId, filter, PageRequest.of(Math.max(0, page), 50));
    }

    @Transactional(readOnly = true)
    public boolean hasLegacy(Long userId) {
        return historyRepository.existsByUserIdAndBatchIdIsNull(userId);
    }

    @Transactional(readOnly = true)
    public Page<ListingSendHistory> getLegacy(Long userId, int page) {
        return historyRepository.findAllByUserIdAndBatchIdIsNullOrderBySentAtDescIdDesc(userId, PageRequest.of(Math.max(0, page), 50));
    }

    private BatchView view(ListingSendBatch batch, ListingSendHistoryRepository.BatchCounts counts) {
        return new BatchView(batch, counts == null ? 0 : counts.getSuccessCount(),
                counts == null ? 0 : counts.getRefundCount(), counts == null ? 0 : counts.getUnknownCount());
    }

    public record BatchView(ListingSendBatch batch, long successCount, long refundCount, long unknownCount) {
        public String getRequestLabel() {
            return switch (batch.getRequestStatus()) {
                case "REQUESTED" -> "발송 준비";
                case "ACCEPTED" -> "접수 완료";
                case "FAILED" -> "요청 실패";
                default -> "접수 여부 확인 필요";
            };
        }
        public boolean isModified() {
            return batch.getListingCreatedAt() != null && batch.getListingUpdatedAt() != null
                    && batch.getListingUpdatedAt().isAfter(batch.getListingCreatedAt());
        }
        public LocalDateTime getListingDate() {
            return isModified() ? batch.getListingUpdatedAt() : batch.getListingCreatedAt();
        }
    }
}
