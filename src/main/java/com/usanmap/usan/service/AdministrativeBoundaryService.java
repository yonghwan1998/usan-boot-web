package com.usanmap.usan.service;

import com.usanmap.usan.dto.BoundaryCodeResponse;
import com.usanmap.usan.dto.PropertyStatDto;
import com.usanmap.usan.dto.RegionInfoDto;
import com.usanmap.usan.dto.RegionLabelDto;
import com.usanmap.usan.entity.AdministrativeBoundary;
import com.usanmap.usan.entity.enums.AdministrativeLevel;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository.RegionLabelProjection;
import com.usanmap.usan.repository.CrawledListingRepository;
import com.usanmap.usan.repository.CrawledListingRepository.ListingTypeStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrativeBoundaryService {

    private final AdministrativeBoundaryRepository administrativeBoundaryRepository;
    private final CrawledListingRepository crawledListingRepository;

    public BoundaryCodeResponse getRegionCodes(double lat, double lng) {
        AdministrativeBoundary sido = administrativeBoundaryRepository
                .findBoundaryContaining(AdministrativeLevel.SIDO.name(), lat, lng)
                .orElse(null);

        AdministrativeBoundary sigungu = administrativeBoundaryRepository
                .findBoundaryContaining(AdministrativeLevel.SIGUNGU.name(), lat, lng)
                .orElse(null);

        AdministrativeBoundary emd = administrativeBoundaryRepository
                .findBoundaryContaining(AdministrativeLevel.EMD.name(), lat, lng)
                .orElse(null);

        return BoundaryCodeResponse.builder()
                .sidoCode(extractSidoCode(sido))
                .sigunguCode(extractSigunguCode(sigungu))
                .emdCode(extractEmdCode(emd))
                .sidoName(extractName(sido))
                .sigunguName(extractName(sigungu))
                .emdName(extractName(emd))
                .build();
    }

    private String extractSidoCode(AdministrativeBoundary boundary) {
        if (boundary == null || boundary.getAdmCd() == null || boundary.getAdmCd().length() < 2) {
            return null;
        }
        return boundary.getAdmCd().substring(0, 2);
    }

    private String extractSigunguCode(AdministrativeBoundary boundary) {
        if (boundary == null || boundary.getAdmCd() == null || boundary.getAdmCd().length() < 5) {
            return null;
        }
        return boundary.getAdmCd().substring(0, 5);
    }

    private String extractEmdCode(AdministrativeBoundary boundary) {
        return boundary != null ? boundary.getAdmCd() : null;
    }

    private String extractName(AdministrativeBoundary boundary) {
        return boundary != null ? boundary.getName() : null;
    }

    public RegionInfoDto getRegionInfo(String admCd) {
        AdministrativeBoundary boundary = administrativeBoundaryRepository.findByAdmCd(admCd).orElse(null);

        String sidoName = null, sigunguName = null, emdName = null, level = null;
        List<ListingTypeStat> stats = List.of();
        long brokerCount = 0;

        if (boundary != null) {
            level = boundary.getAdmLevel().name();
            switch (boundary.getAdmLevel()) {
                case SIDO -> {
                    sidoName = boundary.getName();
                    String sidoCode = admCd.substring(0, 2);
                    stats = crawledListingRepository.countByListingTypeAndSidoCode(sidoCode);
                    brokerCount = crawledListingRepository.countDistinctBrokerBySidoCode(sidoCode);
                }
                case SIGUNGU -> {
                    sigunguName = boundary.getName();
                    if (boundary.getParentAdmCd() != null) {
                        sidoName = administrativeBoundaryRepository.findByAdmCd(boundary.getParentAdmCd())
                                .map(AdministrativeBoundary::getName).orElse(null);
                    }
                    String sigunguCode = admCd.substring(0, 5);
                    stats = crawledListingRepository.countByListingTypeAndSigunguCode(sigunguCode);
                    brokerCount = crawledListingRepository.countDistinctBrokerBySigunguCode(sigunguCode);
                }
                case EMD -> {
                    emdName = boundary.getName();
                    AdministrativeBoundary sigungu = boundary.getParentAdmCd() != null
                            ? administrativeBoundaryRepository.findByAdmCd(boundary.getParentAdmCd()).orElse(null)
                            : null;
                    if (sigungu != null) {
                        sigunguName = sigungu.getName();
                        if (sigungu.getParentAdmCd() != null) {
                            sidoName = administrativeBoundaryRepository.findByAdmCd(sigungu.getParentAdmCd())
                                    .map(AdministrativeBoundary::getName).orElse(null);
                        }
                    }
                    String emdCode = admCd.substring(0, Math.min(10, admCd.length()));
                    stats = crawledListingRepository.countByListingTypeAndEmdCode(emdCode);
                    brokerCount = crawledListingRepository.countDistinctBrokerByEmdCode(emdCode);
                }
            }
        }

        return new RegionInfoDto(level, sidoName, sigunguName, emdName,
                (int) brokerCount, buildPropertyStats(stats));
    }

    private List<PropertyStatDto> buildPropertyStats(List<ListingTypeStat> stats) {
        if (stats.isEmpty()) return List.of();

        Map<String, String> cssClassMap = Map.ofEntries(
                Map.entry("아파트",    "listing__tag--apt"),
                Map.entry("오피스텔",  "listing__tag--officetel"),
                Map.entry("빌라/연립", "listing__tag--villa"),
                Map.entry("원룸",      "listing__tag--oneroom"),
                Map.entry("투룸",      "listing__tag--tworoom"),
                Map.entry("상가",      "listing__tag--store"),
                Map.entry("사무실",    "listing__tag--office"),
                Map.entry("건물",      "listing__tag--building"),
                Map.entry("토지",      "listing__tag--land")
        );

        List<ListingTypeStat> sorted = stats.stream()
                .filter(s -> s.getCount() > 0)
                .sorted(Comparator.comparingLong(ListingTypeStat::getCount).reversed())
                .toList();

        long total = sorted.stream().mapToLong(ListingTypeStat::getCount).sum();

        return sorted.stream()
                .map(s -> new PropertyStatDto(
                        s.getListingType(),
                        cssClassMap.getOrDefault(s.getListingType(), "listing__tag--apt"),
                        (int) s.getCount(),
                        total > 0 ? Math.round(s.getCount() * 100f / total) : 0
                ))
                .toList();

    }

    public List<RegionLabelDto> getRegionLabels(String level, double south, double north, double west, double east) {
        List<RegionLabelProjection> projections = administrativeBoundaryRepository
                .findLabelsByLevelInBounds(level, south, north, west, east);

        return projections.stream()
                .map(p -> new RegionLabelDto(p.getName(), p.getLat(), p.getLng()))
                .toList();
    }
}
