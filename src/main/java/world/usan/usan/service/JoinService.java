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

        if (!req.password().equals(req.passwordConfirm())) {
            throw new IllegalArgumentException("비민번호 확인이 일치하지 않습니다.");
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        if (userRepository.existsByPhone(req.phone())) {
            throw new IllegalArgumentException("이미 가입된 전화번호입니다.");
        }

        User user = userRepository.save(User.builder()
                .email(req.email())
                .phone(req.phone())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .status(User.Status.ACTIVE)
                .build());

        return user.getId();
    }
}
