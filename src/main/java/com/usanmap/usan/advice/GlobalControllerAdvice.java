package com.usanmap.usan.advice;

import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.UserRegionRepository;
import com.usanmap.usan.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.usanmap.usan.security.CustomUserDetails;
import com.usanmap.usan.dto.LoginViewModel;

import java.util.List;

/**
 * @date    2025-12-18
 * @author  yongss
 * @desc    전체 페이지에서 사용 되는 Spring Advice
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final UserRegionRepository userRegionRepository;
    private final SecurityUtils securityUtils;

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
     * @date    2026-01-20
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 헤더를 보여주는 페이지인지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - 현재 uri가 헤더를 보여주는 페이지인지 검증
     *  - 검증된 boolean 값에 따라 헤더 노출/미노출 결정
     *
     * 예외/주의:
     *  - 새로운 허용 페이지 생성 시 메서드에 추가
     */
    @ModelAttribute("showHeader")
    public boolean showHeader(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/index2");// TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
//        return uri.equals("/");
    }

    /**
     * @date    2026-01-20
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 바텀 네비를 보여주는 페이지인지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - 현재 uri가 바텀 네비를 보여주는 페이지인지 검증
     *  - 검증된 boolean 값에 따라 바텀 네비 노출/미노출 결정
     *
     * 예외/주의:
     *  - 새로운 허용 페이지 생성 시 메서드에 추가
     */
    @ModelAttribute("showBottomNav")
    public boolean showBottomNav(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/")
                || uri.equals("/index2")
                || uri.equals("/map");// TODO(yongss): 추후 복구(해당 줄 복사하여 전체 롤백)
//        return uri.equals("/")
//                || uri.equals("/map");
    }

    /**
     * @date    2026-01-10
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 search header를 보여주는 페이지인지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - 현재 uri가 /search 인지 확인
     *  - 반환 된 boolean 값을 전체 페이지에 전달
     *  - boolean 값에 따라 search header 노출/미노출 결정
     */
    @ModelAttribute("showSearchHeader")
    public boolean showSearchHeader(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/search");
    }

    /**
     * @date    2026-01-20
     * @author  yongss
     * @param   {HttpServletRequest request}
     * @return  {boolean} 현재 페이지가 반응형 페이지를 보여줘야하는지 boolean 타입으로 반환
     *
     * 처리 과정:
     *  - 현재 uri가 반응형 페이지를 보여줘야하는 페이지인지 검증
     *  - 검증된 boolean 값에 따라 반응형 페이지 노출/미노출 결정
     */
    @ModelAttribute("showResponsiveWeb")
    public boolean showResponsiveWeb(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/map")
                || uri.equals("/map/listings/share");
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

        Long userId = securityUtils.currentUserId();
        if (userId == null) {
            return LoginViewModel.anonymous();
        }

        String email = null;
        String nickname = null;

        if (principal instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("app_user_email");
            nickname = oAuth2User.getAttribute("app_user_nickname");
        } else if (principal instanceof CustomUserDetails userDetails) {
            email = userDetails.getEmail();
            nickname = userDetails.getNickname();
        }

        List<UserRegion> regions = userRegionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        String nearbyAddr = null;
        if (!regions.isEmpty()) {
            UserRegion r = regions.get(0);
            nearbyAddr = r.getSigunguName() + " " + r.getEmdName();
        }

        return LoginViewModel.authenticated(userId, email, nickname, nearbyAddr);
    }
}
