package com.consuntiver.repository;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface WorkEntryRepository extends JpaRepository<WorkEntry, Long> {

    /** Voci dell'utente in un intervallo temporale, dalla piu' recente alla piu' vecchia. */
    List<WorkEntry> findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(
            User user, Instant from, Instant to);
}
