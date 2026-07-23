package com.consuntiver.repository;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedTaskRepository extends JpaRepository<FixedTask, Long> {

    /** Task ATTIVI dell'utente, anni piu' recenti prima e, a parita' di anno, i piu' nuovi prima. */
    List<FixedTask> findByUserAndDeletedFalseOrderByYearDescIdDesc(User user);

    /** Task ELIMINATI (logicamente) dell'utente, i piu' recenti prima. */
    List<FixedTask> findByUserAndDeletedTrueOrderByYearDescIdDesc(User user);

    /**
     * Cerca il task di un utente per codice e anno (per l'auto-risoluzione dalle attivita').
     * Ignora il flag di eliminazione: c'e' al piu' un task per (utente, codice, anno) e va
     * ritrovato anche se cestinato, per poterlo riattivare.
     */
    Optional<FixedTask> findFirstByUserAndTaskNumberAndYear(User user, String taskNumber, int year);
}
