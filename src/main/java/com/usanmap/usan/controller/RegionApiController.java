package com.usanmap.usan.controller;

import com.usanmap.usan.dto.RegionEmdItemDto;
import com.usanmap.usan.dto.RegionSelectItemDto;
import com.usanmap.usan.service.AdministrativeBoundaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionApiController {

    private final AdministrativeBoundaryService administrativeBoundaryService;

    @GetMapping("/sido")
    public List<RegionSelectItemDto> getSidoList() {
        return administrativeBoundaryService.getSidoList();
    }

    @GetMapping("/sigungu")
    public List<RegionSelectItemDto> getSigunguList(@RequestParam String sidoCd) {
        return administrativeBoundaryService.getSigunguList(sidoCd);
    }

    @GetMapping("/emd")
    public List<RegionEmdItemDto> getEmdList(@RequestParam String sigunguCd) {
        return administrativeBoundaryService.getEmdList(sigunguCd);
    }
}
