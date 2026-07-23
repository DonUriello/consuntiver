package com.consuntiver.repository;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WorkEntryRepository extends JpaRepository<WorkEntry, Long> {

    /** Voci dell'utente iniziate nell'intervallo, dalla piu' recente alla piu' vecchia. */
    List<WorkEntry> findByUserAndStartedAtBetweenOrderByStartedAtDesc(
            User user, Instant from, Instant to);

    /** Tutte le voci dell'utente, dalla piu' recente alla piu' vecchia (per lo storico). */
    List<WorkEntry> findByUserOrderByStartedAtDesc(User user);

    /** L'eventuale voce ancora "in corso" (senza fine) dell'utente. */
    Optional<WorkEntry> findFirstByUserAndEndedAtIsNullOrderByStartedAtDesc(User user);

    /**
     * Sgancia un task da tutte le voci che lo referenziano (id_task = null): serve
     * prima di cancellare un task fisso, per non violare il vincolo di chiave esterna.
     */
    @Modifying
    @Query("update WorkEntry w set w.task = null where w.task = :task")
    int clearTask(@Param("task") FixedTask task);

    void deleteByUser(User user);
}
