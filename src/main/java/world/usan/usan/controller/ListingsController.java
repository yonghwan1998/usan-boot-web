package world.usan.usan.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import world.usan.usan.dto.ListingRequest;

import java.util.List;
import java.util.Map;

@RequestMapping("/listings")
@Controller
public class ListingsController {

    /**
     * @date    2026-01-06
     * @author  yongss
     * @param   {}
     *
     * 처리 과정:
     *  - 매물 관리하기 페이지 이동
     */
    @GetMapping("")
    public String list() {

        return "pages/listings/listings-list";
    }

    /**
     * @date    2025-12-24
     * @author  yongss
     * @param   {Model model}
     *
     * 처리 과정:
     *  - 폼 바인딩용 listingRequest에 초기화 데이터 담아서 전달
     */
    @GetMapping("/new")
    public String newPage(Model model) {
        model.addAttribute("listingRequest",
                new ListingRequest(
                        null, null,
                        "", null, null,
                        null, null,
                        null
                )
        );
        return "pages/listings/listings-new";
    }

    /**
     * @date    2026-01-06
     * @author  yongss
     * @param   {ListingRequest listingRequest}
     *
     * 처리 과정:
     *  - 폼 바인딩용 listingRequest에 담겨 전달 된 데이터를 검증
     *  - 검증 완료 된 데이터 DB에 저장
     *  - 매물 관리하기 페이지로 리다이렉트
     */
    @PostMapping("")
    public String create(@Valid @ModelAttribute("listingRequest") ListingRequest listingRequest,
                         BindingResult bindingResult,
                         Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("listingStep", 1);
            return "pages/listings/listings-new";
        }
        // TODO(yongss): 검증 완료 된 데이터 DB에 저장
        // TODO(yongss): 검증 완료 시 front에 status를 보내 매물 전송 or 매물 관리로 이동할 수 있게

        return "redirect:/listings";
    }

    /**
     * @date    2026-01-07
     * @author  yongss
     * @param   {}
     *
     * 처리 과정:
     *  - publicId로 매물 조회
     *  - 수정 폼 바인딩용 ListingRequest 세팅
     *  - 수정 페이지 렌더링
     */
    @GetMapping("/{publicId}/edit")
    public String editPage(@PathVariable String publicId, Model model) {

        // TODO(yongss): publicId로 Listing 조회
        // TODO(yongss): 조회 결과를 ListingRequest로 매핑
        // TODO(yongss): 조회 실패 시 404 처리

        return "pages/listings/listings-edit";
    }

    @GetMapping("/api/address/search")
    @ResponseBody
    public Map<String, Object> searchAddress(@RequestParam("q") String q) {

        // TODO: 나중에 도로명/지번/키워드 검색 실제 API 붙이면 됨
        // 지금은 더미 5개 (제주도)
        var items = List.of(
                Map.of("name","제주특별자치도청", "roadAddress","제주특별자치도 제주시 문연로 6", "jibunAddress","제주시 연동 312-1", "lat",33.489011, "lng",126.498302),
                Map.of("name","제주국제공항", "roadAddress","제주특별자치도 제주시 공항로 2", "jibunAddress","제주시 용담2동 2002", "lat",33.510414, "lng",126.492200),
                Map.of("name","동문시장", "roadAddress","제주특별자치도 제주시 관덕로14길 20", "jibunAddress","제주시 건입동 1319-1", "lat",33.512467, "lng",126.527053),
                Map.of("name","이호테우해변", "roadAddress","제주특별자치도 제주시 이호일동", "jibunAddress","제주시 이호1동 1665-13", "lat",33.498889, "lng",126.452778),
                Map.of("name","한라수목원", "roadAddress","제주특별자치도 제주시 수목원길 72", "jibunAddress","제주시 연동 1000", "lat",33.469444, "lng",126.496389)
        );

        // 간단 필터: q가 포함된 것만 (나중에 실제 검색으로 교체)
        var filtered = items.stream()
                .filter(m -> {
                    String name = String.valueOf(m.get("name"));
                    String road = String.valueOf(m.get("roadAddress"));
                    String jibun = String.valueOf(m.get("jibunAddress"));
                    return name.contains(q) || road.contains(q) || jibun.contains(q);
                })
                .toList();

        return Map.of("items", filtered);
    }

}
