package com.consuntiver.repository;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkDayRepository extends JpaRepository<WorkDay, Long> {

    /** La giornata lavorativa dell'utente per una certa data, se esiste. */
    Optional<WorkDay> findByUserAndWorkDate(User user, LocalDate workDate);

    /** Tutte le giornate dell'utente, dalla piu' recente alla piu' vecchia. */
    List<WorkDay> findByUserOrderByWorkDateDesc(User user);

    /**
     * Cancellazione bulk (eseguita subito): evita che, durante il re-seeding, Hibernate
     * ordini l'INSERT della nuova giornata prima della DELETE della vecchia, violando il
     * vincolo di unicita' (id_user, work_date).
     */
    @Modifying
    @Query("delete from WorkDay w where w.user = :user")
    void deleteByUser(User user);
}
