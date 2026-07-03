package com.consuntiver.repository;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedTaskRepository extends JpaRepository<FixedTask, Long> {

    /** Task dell'utente, anni piu' recenti prima e, a parita' di anno, i piu' nuovi prima. */
    List<FixedTask> findByUserOrderByYearDescIdDesc(User user);

    /** Cerca il task di un utente per codice e anno (per l'auto-risoluzione dalle attivita'). */
    Optional<FixedTask> findFirstByUserAndTaskNumberAndYear(User user, String taskNumber, int year);
}
