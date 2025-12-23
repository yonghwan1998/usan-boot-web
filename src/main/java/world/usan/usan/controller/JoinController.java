package world.usan.usan.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import world.usan.usan.dto.JoinRequest;
import world.usan.usan.service.JoinService;

/**
 * @date    2025-12-22
 * @author  yongss
 * @desc    회원가입 Controller
 */
@Controller
@RequiredArgsConstructor
public class JoinController {

    private final JoinService joinService;

    /**
     * @date    2025-12-22
     * @author  yongss
     * @param   {Model model}
     *
     * 처리 과정:
     *  - 폼 바인딩용 joinRequest에 초기화 데이터 담아서 전달
     */
    @GetMapping("/join")
    public String joinPage(Model model) {
        model.addAttribute("joinRequest", new JoinRequest("", "", "", "", ""));
        return "pages/join";
    }

    /**
     * @date    2025-12-22
     * @author  yongss
     * @param   {JoinRequest joinRequest, BindingResult bindingResult, Model model}
     * @return  {}
     *
     * 처리 과정:
     *  - View에서 form 데이터 전달 받음
     *  - 서버 검증 후 가입 완료 시 로그인 페이지로 이동
     *
     * 예외/주의:
     *  - 서버 검증 실패 시 실패 데이터에 대해 STEP 다르게 처리 필요하면 추후 관리 필요
     */
    @PostMapping("/join")
    public String joinSubmit(@Valid @ModelAttribute("joinRequest")JoinRequest joinRequest,
                             BindingResult bindingResult,
                             Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("joinError", "입력값을 확인해주세요.");
            return "pages/join";
        }

        try {
            joinService.join(joinRequest);
        } catch (IllegalArgumentException e) {
            model.addAttribute("joinError", e.getMessage());
            return "pages/join";
        }

        return "redirect:/login";
    }
}
