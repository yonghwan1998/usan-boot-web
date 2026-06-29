package com.usanmap.usan.service;

import com.usanmap.usan.repository.RegionStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionStatService {

    private final RegionStatRepository regionStatRepository;

    @Transactional
    public void refresh() {
        regionStatRepository.clearAll();
        regionStatRepository.insertSidoStats();
        regionStatRepository.insertSigunguStats();
        regionStatRepository.insertEmdStats();
    }
}
