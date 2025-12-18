package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.UserSocialAccount;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(UserSocialAccount.Provider provider, String providerUserId);
}
