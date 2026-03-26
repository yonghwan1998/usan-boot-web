package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import world.usan.usan.dto.*;
import world.usan.usan.entity.Listing;
import world.usan.usan.repository.ListingRepository;
import world.usan.usan.security.SecurityUtils;
import world.usan.usan.service.MapService;

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
        return mapService.getBrokerDetails(request.getBrokerCodes());
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
        List<MyListingDto> result = listingRepository.findAllByUserId(userId).stream()
                .map(l -> new MyListingDto(
                        l.getId(),
                        l.getPublicId(),
                        l.getAddressName(),
                        l.getType(),
                        l.getTradeType()))
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

        // TODO(yongss): 실제 발송 이력 저장 (2차 구현)
        return ResponseEntity.ok(Map.of("message", "전송 요청이 완료되었습니다."));
    }
}
