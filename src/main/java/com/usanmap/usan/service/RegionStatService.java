package com.usanmap.usan.service;

import com.usanmap.usan.repository.RegionStatRepository;
import com.usanmap.usan.repository.CrawledListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegionStatService {

    private final RegionStatRepository regionStatRepository;
    private final CrawledListingRepository crawledListingRepository;

    @Transactional
    public void refresh() {
        crawledListingRepository.correctSigunguCodesFromEmd();
        regionStatRepository.clearAll();
        regionStatRepository.insertSidoStats();
        regionStatRepository.insertSigunguStats();
        regionStatRepository.insertEmdStats();
    }
}
