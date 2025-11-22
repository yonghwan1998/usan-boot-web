package world.usan.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import world.usan.usan.dto.BrokerMarkerDto;
import world.usan.usan.service.MapService;

import java.util.List;

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
        return mapService.getMarkersInBounds(south, north, west, east);
    }
}
