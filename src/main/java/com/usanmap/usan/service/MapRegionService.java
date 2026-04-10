package com.usanmap.usan.service;

import com.usanmap.usan.dto.RegionCodeResponse;
import com.usanmap.usan.entity.AdministrativeBoundary;
import com.usanmap.usan.entity.enums.AdministrativeLevel;
import com.usanmap.usan.repository.AdministrativeBoundaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapRegionService {

    private final AdministrativeBoundaryRepository administrativeBoundaryRepository;

    public RegionCodeResponse getRegionCodes(double lat, double lng) {
        AdministrativeBoundary sido = administrativeBoundaryRepository
                .findContainingRegion(AdministrativeLevel.SIDO.name(), lat, lng)
                .orElse(null);

        AdministrativeBoundary sigungu = administrativeBoundaryRepository
                .findContainingRegion(AdministrativeLevel.SIGUNGU.name(), lat, lng)
                .orElse(null);

        AdministrativeBoundary emd = administrativeBoundaryRepository
                .findContainingRegion(AdministrativeLevel.EMD.name(), lat, lng)
                .orElse(null);

        return RegionCodeResponse.builder()
                .sidoCode(extractSidoCode(sido))
                .sigunguCode(extractSigunguCode(sigungu))
                .emdCode(extractCode(emd))
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

    private String extractCode(AdministrativeBoundary boundary) {
        return boundary != null ? boundary.getAdmCd() : null;
    }

    private String extractName(AdministrativeBoundary boundary) {
        return boundary != null ? boundary.getName() : null;
    }
}