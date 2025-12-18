package world.usan.usan.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import world.usan.usan.dto.LoginViewModel;

import java.util.Map;

/**
 * @date    2025-12-18
 * @author  yongss
 * @desc    전체 페이지에서 사용 되는 Spring Advice
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    /**
     * @date    2025-12-17
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {HttpServletRequest.getRequestURI()} 현재 페이지의 uri
     */
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * @date    2025-12-18
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 헤더를 보여주는 페이지인지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - isAuthPage(String uri)의 인자로 현재 uri 전달
     *  - 반환 된 boolean 값을 전체 페이지에 전달
     *  - boolean 값에 따라 헤더 노출/미노출 결정
     */
    @ModelAttribute("showHeader")
    public boolean showHeader(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return isAuthPage(uri);
    }

    /**
     * @date    2025-12-18
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 바텀 네비를 보여주는 페이지인지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - isAuthPage(String uri)의 인자로 현재 uri 전달
     *  - 반환 된 boolean 값을 전체 페이지에 전달
     *  - boolean 값에 따라 바텀 네비 노출/미노출 결정
     */
    @ModelAttribute("showBottomNav")
    public boolean showBottomNav(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return isAuthPage(uri);
    }

    /**
     * @date    2025-12-18
     * @author  yongss
     * @param   {String uri}
     * @return  {boolean} 현재 uri가 헤더, 바텀 네비를 보여줘야 하는지 여부를 boolean으로 전달
     *
     * 처리 과정:
     *  - 전달 받은 인자가 미리 정의한 uri와 동일한지 판단
     *  - boolean 값 전달
     *
     * 예외/주의:
     *  - index, 지도, 게시판 등의 페이지를 제외하고 false가 반환 되게
     *  - 새로운 허용 페이지 생성 시 메서드에 추가
     */
    private boolean isAuthPage(String uri) {
        return uri.equals("/")
                || uri.equals("/map");
    }

    /**
     * @date    2025-12-18
     * @author  yongss
     * @return  {LoginViewModel}
     *
     * 처리 과정:
     *  - 현재 로그인 상태인지 확인
     *  - 미로그인 시: LoginViewModel.anonymous() 반환
     *  - 로그인 시: LoginViewModel에 {authenticated, userId, email, nickname, nearbyAddr} 담아서 반환
     *
     * 예외/주의:
     *  - 새로 사용해야할 데이터 있으면 LoginViewModel 도 함께 바꾸기
     */
    @ModelAttribute("login")
    public LoginViewModel loginModel() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return LoginViewModel.anonymous();
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            return LoginViewModel.authenticated(
                    oAuth2User.getAttribute("app_user_id"),
                    oAuth2User.getAttribute("app_user_email"),
                    oAuth2User.getAttribute("app_user_nickname"),
                    "오산시 원동"
            );
        }

        return LoginViewModel.anonymous();
    }
}
