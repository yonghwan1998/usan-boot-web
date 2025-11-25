package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import world.usan.usan.dto.BrokerClusterRequest;
import world.usan.usan.dto.BrokerMarkerDetailDto;
import world.usan.usan.dto.BrokerMarkerDto;
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
}
