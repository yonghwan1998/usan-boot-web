package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import world.usan.usan.dto.ListingRequest;

@RequestMapping("/listings")
@Controller
public class ListingController {

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

        return "pages/listing/listing-list";
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
        model.addAttribute("listingRequest", new ListingRequest(null, null, null));
        return "pages/listing/listing-new";
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
    public String create(ListingRequest listingRequest) {

        // TODO(yongss): 요청 데이터 검증
        // TODO(yongss): 검증 완료 된 데이터 DB에 저장
        // TODO(yongss): 검증 완료 시 front에 status를 보내 매물 전송 or 매물 관리로 이동할 수 있게

        return "redirect:/listings";
    }

}
