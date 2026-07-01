package com.consuntiver.repository;

import com.consuntiver.model.Attendance;
import com.consuntiver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /** Timbrature dell'utente entrate nell'intervallo, in ordine cronologico. */
    List<Attendance> findByUserAndClockInBetweenOrderByClockInAsc(
            User user, Instant from, Instant to);

    /** L'eventuale sessione ancora aperta (senza uscita) dell'utente. */
    Optional<Attendance> findFirstByUserAndClockOutIsNullOrderByClockInDesc(User user);

    void deleteByUser(User user);
}
