package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.dto.JoinRequest;
import world.usan.usan.entity.User;
import world.usan.usan.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long join(JoinRequest req) {

        String normalizedPhone = req.phone().replaceAll("[^0-9]", "");

        // 비밀번호 확인
        if (!req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인을 검증해 주세요.");
        }

        // 이메일 중복
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 전화번호 중복
        if (userRepository.existsByPhone(req.phone())) {
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
        }

        User user = userRepository.save(User.builder()
                .email(req.email())
                .phone(normalizedPhone)
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .status(User.Status.ACTIVE)
                .build());

        return user.getId();
    }
}
