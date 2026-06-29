package com.usanmap.usan.service;

import com.usanmap.usan.dto.*;
import com.usanmap.usan.entity.AdministrativeBoundary;
import com.usanmap.usan.entity.RegionStat;
import com.usanmap.usan.entity.enums.AdministrativeLevel;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository.EmdItemProjection;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository.RegionLabelProjection;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository.RegionSelectProjection;
import com.usanmap.usan.repository.RegionStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrativeBoundaryService {

    private final AdministrativeBoundaryRepository administrativeBoundaryRepository;
    private final RegionStatRepository regionStatRepository;

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
        if (boundary == null) return new RegionInfoDto(null, null, null, null, 0, List.of());

        RegionStat stat = regionStatRepository.findById(admCd).orElse(null);

        String sidoName = null, sigunguName = null, emdName = null;

        switch (boundary.getAdmLevel()) {
            case SIDO -> sidoName = boundary.getName();
            case SIGUNGU -> {
                sigunguName = boundary.getName();
                sidoName = administrativeBoundaryRepository
                        .findFirstByAdmLevelAndAdmCdStartingWith(AdministrativeLevel.SIDO, admCd.substring(0, 2))
                        .map(AdministrativeBoundary::getName).orElse(null);
            }
            case EMD -> {
                emdName = boundary.getName();
                sigunguName = administrativeBoundaryRepository
                        .findFirstByAdmLevelAndAdmCdStartingWith(AdministrativeLevel.SIGUNGU, admCd.substring(0, 5))
                        .map(AdministrativeBoundary::getName).orElse(null);
                sidoName = administrativeBoundaryRepository
                        .findFirstByAdmLevelAndAdmCdStartingWith(AdministrativeLevel.SIDO, admCd.substring(0, 2))
                        .map(AdministrativeBoundary::getName).orElse(null);
            }
        }

        int brokerCount = stat != null ? stat.getBrokerCount() : 0;
        List<PropertyStatDto> propertyStats = stat != null ? buildPropertyStats(stat) : List.of();

        return new RegionInfoDto(boundary.getAdmLevel().name(), sidoName, sigunguName, emdName, brokerCount, propertyStats);
    }

    private static final Map<String, String> CSS_CLASS_MAP = Map.ofEntries(
            Map.entry("아파트",         "listing__tag--apt"),
            Map.entry("오피스텔",       "listing__tag--officetel"),
            Map.entry("빌라/연립",      "listing__tag--villa"),
            Map.entry("원룸",           "listing__tag--oneroom"),
            Map.entry("투룸",           "listing__tag--tworoom"),
            Map.entry("단독/다가구",    "listing__tag--detached"),
            Map.entry("전원주택",       "listing__tag--rural"),
            Map.entry("상가주택",       "listing__tag--mixedhouse"),
            Map.entry("한옥주택",       "listing__tag--hanok"),
            Map.entry("상가",           "listing__tag--store"),
            Map.entry("사무실",         "listing__tag--office"),
            Map.entry("건물",           "listing__tag--building"),
            Map.entry("공장/창고",      "listing__tag--factory"),
            Map.entry("지식산업센터",   "listing__tag--knowledge"),
            Map.entry("토지",           "listing__tag--land"),
            Map.entry("아파트분양권",   "listing__tag--apt-sale"),
            Map.entry("오피스텔분양권", "listing__tag--officetel-sale"),
            Map.entry("재개발",         "listing__tag--redevelopment"),
            Map.entry("재건축",         "listing__tag--reconstruction"),
            Map.entry("분양중/예정",    "listing__tag--presale")
    );

    private List<PropertyStatDto> buildPropertyStats(RegionStat stat) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("아파트",         stat.getAptCnt());
        counts.put("오피스텔",       stat.getOfficetelCnt());
        counts.put("빌라/연립",      stat.getVillaCnt());
        counts.put("원룸",           stat.getOneroomCnt());
        counts.put("투룸",           stat.getTworoomCnt());
        counts.put("단독/다가구",    stat.getDetachedCnt());
        counts.put("전원주택",       stat.getRuralCnt());
        counts.put("상가주택",       stat.getMixedhouseCnt());
        counts.put("한옥주택",       stat.getHanokCnt());
        counts.put("상가",           stat.getStoreCnt());
        counts.put("사무실",         stat.getOfficeCnt());
        counts.put("건물",           stat.getBuildingCnt());
        counts.put("공장/창고",      stat.getFactoryCnt());
        counts.put("지식산업센터",   stat.getKnowledgeCnt());
        counts.put("토지",           stat.getLandCnt());
        counts.put("아파트분양권",   stat.getAptSaleCnt());
        counts.put("오피스텔분양권", stat.getOfficetelSaleCnt());
        counts.put("재개발",         stat.getRedevelopmentCnt());
        counts.put("재건축",         stat.getReconstructionCnt());
        counts.put("분양중/예정",    stat.getPresaleCnt());

        List<Map.Entry<String, Integer>> sorted = counts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .toList();

        long total = sorted.stream().mapToLong(Map.Entry::getValue).sum();

        return sorted.stream()
                .map(e -> new PropertyStatDto(
                        e.getKey(),
                        CSS_CLASS_MAP.getOrDefault(e.getKey(), "listing__tag--apt"),
                        e.getValue(),
                        total > 0 ? Math.round(e.getValue() * 100f / total) : 0
                ))
                .toList();
    }

    public List<RegionSelectItemDto> getSidoList() {
        return administrativeBoundaryRepository
                .findNamesByLevel(AdministrativeLevel.SIDO.name())
                .stream()
                .map(p -> new RegionSelectItemDto(p.getAdmCd(), p.getName()))
                .toList();
    }

    public List<RegionSelectItemDto> getSigunguList(String sidoCd) {
        String parentCd = sidoCd.substring(0, Math.min(2, sidoCd.length()));
        return administrativeBoundaryRepository
                .findNamesByLevelAndParent(AdministrativeLevel.SIGUNGU.name(), parentCd)
                .stream()
                .map(p -> new RegionSelectItemDto(p.getAdmCd(), p.getName()))
                .toList();
    }

    public List<RegionEmdItemDto> getEmdList(String sigunguCd) {
        String parentCd = sigunguCd.substring(0, Math.min(5, sigunguCd.length()));
        return administrativeBoundaryRepository
                .findEmdListByParentAdmCd(parentCd)
                .stream()
                .map(p -> new RegionEmdItemDto(p.getAdmCd(), p.getName(), p.getLat(), p.getLng()))
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
