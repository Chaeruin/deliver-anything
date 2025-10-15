package com.deliveranything.domain.user.user.repository;

import com.deliveranything.domain.auth.auth.enums.SocialProvider;
import com.deliveranything.domain.user.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.currentActiveProfile WHERE u.id = :id")
  Optional<User> findByIdWithProfile(@Param("id") Long id);

  Optional<User> findBySocialProviderAndSocialId(SocialProvider socialProvider, String socialId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByPhoneNumber(String phoneNumber);

}