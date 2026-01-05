package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import world.usan.usan.dto.ListingRequest;
import world.usan.usan.entity.enums.ListingRole;
import world.usan.usan.entity.enums.ListingType;

@RequestMapping("/listings")
@Controller
public class ListingController {

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

}
