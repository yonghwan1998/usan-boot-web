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

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

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
