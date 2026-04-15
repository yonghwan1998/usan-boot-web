package com.usanmap.usan.service;

import com.usanmap.usan.dto.BoundaryCodeResponse;
import com.usanmap.usan.dto.RegionLabelDto;
import com.usanmap.usan.entity.AdministrativeBoundary;
import com.usanmap.usan.entity.enums.AdministrativeLevel;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository.RegionLabelProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdministrativeBoundaryService {

    private final AdministrativeBoundaryRepository administrativeBoundaryRepository;

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

    public List<RegionLabelDto> getRegionLabels(String level, double south, double north, double west, double east) {
        List<RegionLabelProjection> projections = administrativeBoundaryRepository
                .findLabelsByLevelInBounds(level, south, north, west, east);

        return projections.stream()
                .map(p -> new RegionLabelDto(p.getName(), p.getLat(), p.getLng()))
                .toList();
    }
}
