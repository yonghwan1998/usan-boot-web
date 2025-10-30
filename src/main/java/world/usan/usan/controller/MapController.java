package world.usan.usan.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class MapController {

    @Value("${NAVER_MAP_CLIENT_ID}")
    private String naverClientId;

    @GetMapping("/map")
    public String map(Model model) {
        model.addAttribute("naverClientId", naverClientId);
        return "pages/map";
    }

    @GetMapping("/map/mock")
    public String mapMock(Model model) {
        List<Map<String, Object>> agencies = List.of(
            Map.of(
                    "type", "오피스텔",
                    "officeName", "금나무공인중개사사무소",
                    "agentName", "박써니",
                    "address", "제주특별자치도 제주시 노형동 910-5 1층",
                    "regNo", "50110-2019-00029",
                    "tel", "010-2123-7797",
                    "mobile", "010-2123-7797",
                    "latitude", 33.4895,
                    "longitude", 126.4870
            ),
            Map.of(
                    "type", "아파트",
                    "officeName", "부영부동산공인중개사사무소",
                    "agentName", "현혜원",
                    "address", "제주특별자치도 제주시 과원북2길 18, 상가동 104호(노형동, 부영5차아파트)",
                    "regNo", "50110-2021-00090",
                    "tel", "064-743-9900",
                    "mobile", "010-7179-2998",
                    "latitude", 33.4935,
                    "longitude", 126.4765
            ),
            Map.of(
                    "type", "아파트",
                    "officeName", "도남해모로탐라공인중개사사무소",
                    "agentName", "부선욱",
                    "address", "제주특별자치도 제주시 도남서길 13, 1층 (도남동)",
                    "regNo", "50110-2017-00034",
                    "tel", "010-8266-1715",
                    "mobile", "010-8266-1715",
                    "latitude", 33.4939,
                    "longitude", 126.5290
            ),
            Map.of(
                    "type", "아파트분양권",
                    "officeName", "차공인중개사사무소",
                    "agentName", "차정선",
                    "address", "제주특별자치도 제주시 과원로 49 1층 차공인중개사사무소",
                    "regNo", "50110-2024-00022",
                    "tel", "010-4032-8254",
                    "mobile", "010-4032-8254",
                    "latitude", 33.4982,
                    "longitude", 126.4769
            ),
            Map.of(
                    "type", "아파트",
                    "officeName", "제주삼화지구부동산중개사무소",
                    "agentName", "고현영",
                    "address", "제주 제주시 동화로 34(화북일동)",
                    "regNo", "50110-2021-00179",
                    "tel", "010-3837-0650",
                    "mobile", "064-755-0650",
                    "latitude", 33.5165,
                    "longitude", 126.5530
            )
        );


        model.addAttribute("naverClientId", naverClientId);
        model.addAttribute("agencies", agencies);

        return "pages/map-mock";
    }
}
