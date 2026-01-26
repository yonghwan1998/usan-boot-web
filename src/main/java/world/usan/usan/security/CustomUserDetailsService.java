package world.usan.usan.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import world.usan.usan.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {

        var user = userRepository.findByEmail(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("Password login not allowed for this user.");
        }

        return new CustomUserDetails(user);
    }
}
