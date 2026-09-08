package com.usanmap.usan.service;

import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.UserRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserRegionService {

    private final UserRegionRepository userRegionRepository;

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
