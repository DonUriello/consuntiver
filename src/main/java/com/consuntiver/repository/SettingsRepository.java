package com.consuntiver.repository;

import com.consuntiver.model.Settings;
import com.consuntiver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingsRepository extends JpaRepository<Settings, Long> {

    List<Settings> findByUser(User user);

    Optional<Settings> findByUserAndCode(User user, String code);
}
