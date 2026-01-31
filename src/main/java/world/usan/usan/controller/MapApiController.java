package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import world.usan.usan.dto.BrokerClusterRequest;
import world.usan.usan.dto.BrokerMarkerDetailDto;
import world.usan.usan.dto.BrokerMarkerDto;
import world.usan.usan.dto.MyListingDto;
import world.usan.usan.service.MapService;

import java.util.List;
import java.util.UUID;

@RequestMapping("/map/api")
@RestController
@RequiredArgsConstructor
public class MapApiController {

    private final MapService mapService;

    @GetMapping("/brokers")
    public List<BrokerMarkerDto> getBrokersInBounds(
            @RequestParam double south,
            @RequestParam double north,
            @RequestParam double west,
            @RequestParam double east
    ) {
        return mapService.getBrokersInBounds(south, north, west, east);
    }

    @GetMapping("/broker/{brokerCode}")
    public BrokerMarkerDetailDto getBrokerDetail(@PathVariable("brokerCode")UUID brokerCode) {
        return mapService.getBrokerDetail(brokerCode);
    }

    @PostMapping("/brokers/cluster")
    public List<BrokerMarkerDetailDto> getBrokerDetailInCluster(@RequestBody BrokerClusterRequest request) {

        List<UUID> brokerCodes = request.getBrokerCodes();

        return mapService.getBrokerDetails(brokerCodes);
    }

    @GetMapping("/listings/my-listings")
    @ResponseBody
    public List<MyListingDto> getMyListings() {
        // TODO(yongss): 실제 DB 데이터로 변경 필요
        return List.of(
                new MyListingDto(1L, 33.499621, 126.531188, "매물1"),
                new MyListingDto(2L, 33.512312, 126.520987, "매물2"),
                new MyListingDto(3L, 33.489845, 126.488223, "매물3"),
                new MyListingDto(4L, 33.246305, 126.565752, "매물4"),
                new MyListingDto(5L, 33.463912, 126.310447, "매물5")
        );
    }

}
