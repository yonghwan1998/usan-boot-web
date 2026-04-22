package com.usanmap.usan.controller;

import com.usanmap.usan.service.AdministrativeBoundarySeedService;
import com.usanmap.usan.service.AdministrativeBoundaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.usanmap.usan.dto.*;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.ListingRepository;
import com.usanmap.usan.repository.UserRegionRepository;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.MapService;
import com.usanmap.usan.service.SmsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/map/api")
@RestController
@RequiredArgsConstructor
public class MapApiController {

    private final MapService        mapService;
    private final SecurityUtils     securityUtils;
    private final ListingRepository listingRepository;
    private final UserRegionRepository userRegionRepository;
    private final SmsService        smsService;
    private final AdministrativeBoundaryService administrativeBoundaryService;
    private final AdministrativeBoundarySeedService administrativeBoundarySeedService;

    @GetMapping("/brokers")
    public List<BrokerMarkerDto> getBrokersInBounds(
            @RequestParam double south,
            @RequestParam double north,
            @RequestParam double west,
            @RequestParam double east) {
        return mapService.getBrokersInBounds(south, north, west, east);
    }

    @GetMapping("/broker/{brokerCode}")
    public BrokerMarkerDetailDto getBrokerDetail(@PathVariable UUID brokerCode) {
        return mapService.getBrokerDetail(brokerCode);
    }

    @PostMapping("/brokers/cluster")
    public List<BrokerMarkerDetailDto> getBrokerDetailInCluster(@RequestBody BrokerClusterRequest request) {
        return mapService.getBrokerDetails(request.getBrokerCodes(), request.getLat(), request.getLng(), request.getDistanceM(), request.getListingTypes());
    }

    @GetMapping("/user-region")
    public ResponseEntity<?> getUserRegion() {
        Long userId = securityUtils.currentUserId();
        if (userId == null) {
            return ResponseEntity.ok(null);
        }
        List<UserRegion> regions = userRegionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (regions.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        UserRegion region = regions.get(0);
        return ResponseEntity.ok(Map.of("lat", region.getEmdLat(), "lng", region.getEmdLng()));
    }

    /**
     * 로그인 사용자의 매물 목록 조회
     */
    @GetMapping("/listings/my-listings")
    public ResponseEntity<?> getMyListings() {
        Long userId = securityUtils.currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        List<MyListingDto> result = listingRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(l -> new MyListingDto(
                        l.getId(),
                        l.getPublicId(),
                        l.getAddressName(),
                        l.getType(),
                        l.getTradeType(),
                        l.getLat(),
                        l.getLng()))
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 선택한 매물을 선택한 중개사들에게 전송
     * MVP: 소유권 검증까지, 실제 저장은 2차 구현
     */
    @PostMapping("/listings/share")
    public ResponseEntity<Map<String, String>> shareListing(@RequestBody ListingShareRequest request) {
        Long userId = securityUtils.currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        if (request.brokerCodes() == null || request.brokerCodes().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "중개사를 선택해주세요."));
        }

        Listing listing = listingRepository.findById(request.listingId()).orElse(null);
        if (listing == null || !listing.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "유효하지 않은 매물입니다."));
        }

        smsService.sendListingShare(request.brokerCodes(), listing);
        // TODO(yongss): 발송 이력 저장 (2차 구현)
        return ResponseEntity.ok(Map.of("message", "전송 요청이 완료되었습니다."));
    }

    @GetMapping("/region-code")
    public BoundaryCodeResponse getRegionCodes(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return administrativeBoundaryService.getRegionCodes(lat, lng);
    }

    @GetMapping("/region-info")
    public RegionInfoDto getRegionInfo(@RequestParam String admCd) {
        return administrativeBoundaryService.getRegionInfo(admCd);
    }

    @GetMapping("/region-labels")
    public List<RegionLabelDto> getRegionLabels(
            @RequestParam String level,
            @RequestParam double south,
            @RequestParam double north,
            @RequestParam double west,
            @RequestParam double east
    ) {
        return administrativeBoundaryService.getRegionLabels(level, south, north, west, east);
    }

    @PostMapping("/admin/boundaries/seed/all")
    public ResponseEntity<BoundarySeedResponse> seedAll() {
        return ResponseEntity.ok(administrativeBoundarySeedService.seedAll());
    }
}
