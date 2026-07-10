package com.consuntiver.repository;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkDayRepository extends JpaRepository<WorkDay, Long> {

    /** La giornata lavorativa dell'utente per una certa data, se esiste. */
    Optional<WorkDay> findByUserAndWorkDate(User user, LocalDate workDate);

    void deleteByUser(User user);
}
