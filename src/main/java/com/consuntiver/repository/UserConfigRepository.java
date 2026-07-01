package com.consuntiver.repository;

import com.consuntiver.model.User;
import com.consuntiver.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserConfigRepository extends JpaRepository<UserConfig, Long> {

    Optional<UserConfig> findByUser(User user);
}
