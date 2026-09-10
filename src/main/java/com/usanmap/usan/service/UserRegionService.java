package com.usanmap.usan.service;

import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.UserRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRegionService {

    private final UserRegionRepository userRegionRepository;

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getCoordinates(Long userId) {
        List<UserRegion> regions = userRegionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (regions.isEmpty()) {
            return null;
        }
        UserRegion region = regions.get(0);
        return Map.of("lat", region.getEmdLat(), "lng", region.getEmdLng());
    }

    @Transactional
    public void saveOrUpdate(Long userId,
                             String admCd,
                             String sidoName,
                             String sigunguName,
                             String emdName,
                             BigDecimal emdLat,
                             BigDecimal emdLng) {
        userRegionRepository.findByUserId(userId).ifPresentOrElse(
                existing -> existing.update(admCd, sidoName, sigunguName, emdName, emdLat, emdLng),
                () -> userRegionRepository.save(UserRegion.builder()
                        .userId(userId)
                        .admCd(admCd)
                        .sidoName(sidoName)
                        .sigunguName(sigunguName)
                        .emdName(emdName)
                        .emdLat(emdLat)
                        .emdLng(emdLng)
                        .build())
        );
    }
}
