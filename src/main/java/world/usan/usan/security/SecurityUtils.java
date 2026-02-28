package world.usan.usan.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Long currentUserIdOrThrow() {

        Long id = currentUserId();
        if (id == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        return id;
    }

    public Long currentUserId() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal == null) {
            return null;
        }

        if (principal instanceof String s && "anonymousUser".equals(s)) {
            return null;
        }

        if (principal instanceof CustomUserDetails cud) {
            return cud.getUserId();
        }

        if (principal instanceof OAuth2User ou) {

            Object v = ou.getAttributes().get("app_user_id");
            if (v == null) {
                return null;
            }
            if (v instanceof Number n) {
                return n.longValue();
            }

            return Long.valueOf(String.valueOf(v));
        }

        return null;
    }
}
