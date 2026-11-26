package com.usanmap.usan.service;

import com.usanmap.usan.entity.ListingLinkVisit;
import com.usanmap.usan.repository.ListingLinkVisitRepository;
import com.usanmap.usan.repository.ListingSendHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListingLinkVisitService {
    private final ListingSendHistoryRepository histories;
    private final ListingLinkVisitRepository visits;

    @Transactional
    public void record(Long listingId, String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{6}")) return;
        histories.findByTrackingTokenAndListingId(token, listingId)
                .filter(history -> token.equals(history.getTrackingToken())).ifPresent(history ->
                visits.save(ListingLinkVisit.builder().historyId(history.getId()).visitedAt(LocalDateTime.now()).build()));
    }

    @Transactional(readOnly = true)
    public Map<Long, ListingLinkVisitRepository.RecipientVisits> recipients(Long userId, List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return visits.summarizeRecipients(userId, ids).stream()
                .collect(Collectors.toMap(ListingLinkVisitRepository.RecipientVisits::getHistoryId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Map<Long, ListingLinkVisitRepository.BatchVisits> batches(Long userId, List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return visits.summarizeBatches(userId, ids).stream()
                .collect(Collectors.toMap(ListingLinkVisitRepository.BatchVisits::getBatchId, Function.identity()));
    }
}
