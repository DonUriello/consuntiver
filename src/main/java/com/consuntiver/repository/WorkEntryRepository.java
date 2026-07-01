package com.consuntiver.repository;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkEntryRepository extends JpaRepository<WorkEntry, Long> {

    /** Voci dell'utente iniziate nell'intervallo, dalla piu' recente alla piu' vecchia. */
    List<WorkEntry> findByUserAndStartedAtBetweenOrderByStartedAtDesc(
            User user, Instant from, Instant to);

    /** L'eventuale voce ancora "in corso" (senza fine) dell'utente. */
    Optional<WorkEntry> findFirstByUserAndEndedAtIsNullOrderByStartedAtDesc(User user);
}
