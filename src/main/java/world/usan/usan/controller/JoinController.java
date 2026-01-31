package world.usan.usan.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import world.usan.usan.dto.JoinRequest;
import world.usan.usan.repository.UserRepository;
import world.usan.usan.service.JoinService;

import java.util.Map;

/**
 * @date    2025-12-22
 * @author  yongss
 * @desc    회원가입 Controller
 */
@Controller
@RequestMapping("/join")
@RequiredArgsConstructor
public class JoinController {

    private final JoinService joinService;
    private final UserRepository userRepository;

    /**
     * @date    2025-12-22
     * @author  yongss
     * @param   {Model model}
     *
     * 처리 과정:
     *  - 폼 바인딩용 joinRequest에 초기화 데이터 담아서 전달
     */
    @GetMapping("")
    public String joinPage(Model model) {
        model.addAttribute("joinRequest", new JoinRequest("", "", "", "", ""));
        return "pages/join";
    }

    /**
     * @date    2026-01-31
     * @author  yongss
     * @param   {String email}
     *
     * 처리 과정:
     *  - email을 전달 받아 DB에 존재하는지 AJAX 검증
     */
    @GetMapping("/api/email-exists")
    @ResponseBody
    public Map<String, Object> emailExists(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);
        return Map.of("exists", exists);
    }

    /**
     * @date    2026-01-31
     * @author  yongss
     * @param   {String phone}
     *
     * 처리 과정:
     *  - phone을 전달 받아 DB에 존재하는지 AJAX 검증
     */
    @GetMapping("/api/phone-exists")
    @ResponseBody
    public Map<String, Object> phoneExists(@RequestParam String phone) {

        String normalizedPhone = phone.replaceAll("[^0-9]", "");

        boolean exists = userRepository.existsByPhone(normalizedPhone);
        return Map.of("exists", exists);
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
    @PostMapping("")
    public String joinSubmit(@Valid @ModelAttribute("joinRequest")JoinRequest joinRequest,
                             BindingResult bindingResult,
                             Model model) {

        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            model.addAttribute("joinError", msg);
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
