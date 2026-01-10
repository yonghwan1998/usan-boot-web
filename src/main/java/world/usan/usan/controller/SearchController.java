package world.usan.usan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/search")
@Controller
public class SearchController {

    /**
     * @date    2026-01-10
     * @author  yongss
     * @param   {}
     *
     * 처리 과정:
     *  - 검색 페이지 이동
     */
    @GetMapping("")
    public String list() {
        return "pages/search";
    }
}
