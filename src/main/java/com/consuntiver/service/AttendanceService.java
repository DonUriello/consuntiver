package com.consuntiver.service;

import com.consuntiver.model.Attendance;
import com.consuntiver.model.User;
import com.consuntiver.repository.AttendanceRepository;
import com.consuntiver.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
    }

    /** Registra un'entrata. Se c'e' gia' una sessione aperta non fa nulla. */
    public void clockIn(String username) {
        User user = requireUser(username);
        if (attendanceRepository.findFirstByUserAndClockOutIsNullOrderByClockInDesc(user).isPresent()) {
            return; // gia' "dentro"
        }
        attendanceRepository.save(new Attendance(Instant.now(), user));
    }

    /** Registra un'uscita chiudendo la sessione aperta. Se non c'e' non fa nulla. */
    public void clockOut(String username) {
        User user = requireUser(username);
        attendanceRepository.findFirstByUserAndClockOutIsNullOrderByClockInDesc(user)
                .ifPresent(open -> {
                    open.setClockOut(Instant.now());
                    attendanceRepository.save(open);
                });
    }

    /** Timbrature di oggi, in ordine cronologico. */
    public List<Attendance> todayAttendances(String username, ZoneId zone) {
        User user = requireUser(username);
        Instant[] bounds = dayBounds(zone);
        return attendanceRepository.findByUserAndClockInBetweenOrderByClockInAsc(user, bounds[0], bounds[1]);
    }

    /** Riepilogo del tempo lavorato oggi per alimentare il contatore live. */
    public WorkTimeSummary todaySummary(String username, ZoneId zone) {
        List<Attendance> attendances = todayAttendances(username, zone);
        long accumulatedSeconds = 0;
        Long openSinceMillis = null;
        for (Attendance a : attendances) {
            if (a.getClockOut() != null) {
                accumulatedSeconds += Duration.between(a.getClockIn(), a.getClockOut()).getSeconds();
            } else {
                openSinceMillis = a.getClockIn().toEpochMilli();
            }
        }
        return new WorkTimeSummary(accumulatedSeconds, openSinceMillis);
    }

    private Instant[] dayBounds(ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        return new Instant[]{from, to};
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /**
     * Riepilogo del tempo lavorato oggi.
     *
     * @param accumulatedSeconds secondi gia' completati (sessioni chiuse)
     * @param openSinceMillis     epoch-millis dell'entrata aperta, oppure null se non si e' "dentro"
     */
    public record WorkTimeSummary(long accumulatedSeconds, Long openSinceMillis) {

        public boolean clockedIn() {
            return openSinceMillis != null;
        }
    }
}
