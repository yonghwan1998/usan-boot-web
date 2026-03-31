package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.UserSocialAccount;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(UserSocialAccount.Provider provider, String providerUserId);
}
