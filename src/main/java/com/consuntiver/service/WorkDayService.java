package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkDay;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkDayRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class WorkDayService {

    /** Obiettivo giornaliero: 8 ore di lavoro netto, in secondi. */
    public static final long TARGET_SECONDS = 8 * 60 * 60;
    /** Pausa pranzo assunta di default (1 ora) finche' non se ne inseriscono gli orari. */
    public static final long DEFAULT_BREAK_SECONDS = 60 * 60;
    /** Pausa pranzo minima: una pausa piu' breve di 45 minuti vale comunque 45 minuti. */
    public static final long MIN_BREAK_SECONDS = 45 * 60;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final WorkDayRepository workDayRepository;
    private final UserRepository userRepository;

    public WorkDayService(WorkDayRepository workDayRepository, UserRepository userRepository) {
        this.workDayRepository = workDayRepository;
        this.userRepository = userRepository;
    }

    /** La giornata di oggi come vista pronta per il template e per il timer JS. */
    public WorkDayView today(String username, ZoneId zone) {
        User user = requireUser(username);
        LocalDate today = LocalDate.now(zone);
        WorkDay day = workDayRepository.findByUserAndWorkDate(user, today).orElse(null);
        return WorkDayView.of(day, zone);
    }

    /**
     * Salva (o aggiorna) i quattro orari di oggi. Ogni valore e' nel formato "HH:mm";
     * una stringa vuota o non valida azzera il corrispondente orario.
     */
    public void saveTimes(String username, ZoneId zone,
                          String entry, String lunchStart, String lunchEnd, String exit) {
        User user = requireUser(username);
        LocalDate today = LocalDate.now(zone);
        WorkDay day = workDayRepository.findByUserAndWorkDate(user, today)
                .orElseGet(() -> new WorkDay(today, user));

        day.setEntryAt(parse(entry, today, zone));
        day.setLunchStartAt(parse(lunchStart, today, zone));
        day.setLunchEndAt(parse(lunchEnd, today, zone));
        day.setExitAt(parse(exit, today, zone));

        workDayRepository.save(day);
    }

    /**
     * Secondi di lavoro netto di una giornata conclusa (servono sia entrata sia uscita):
     * (uscita - entrata) meno la pausa pranzo, che vale comunque almeno {@link #MIN_BREAK_SECONDS}.
     * Restituisce null se la giornata non ha entrata e uscita.
     */
    public static Long netWorkedSeconds(WorkDay day) {
        if (day == null || day.getEntryAt() == null || day.getExitAt() == null) {
            return null;
        }
        long gross = Duration.between(day.getEntryAt(), day.getExitAt()).getSeconds();
        if (day.getLunchStartAt() != null && day.getLunchEndAt() != null) {
            long lunch = Duration.between(day.getLunchStartAt(), day.getLunchEndAt()).getSeconds();
            gross -= Math.max(lunch, MIN_BREAK_SECONDS);
        }
        return Math.max(0, gross);
    }

    /** Converte "HH:mm" in un istante sul giorno indicato; null se vuoto/non valido. */
    private Instant parse(String hhmm, LocalDate day, ZoneId zone) {
        if (hhmm == null || hhmm.isBlank()) {
            return null;
        }
        try {
            LocalTime time = LocalTime.parse(hhmm.trim(), HHMM);
            return day.atTime(time).atZone(zone).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /**
     * Vista della giornata: orari come "HH:mm" per il form e come epoch-millis per il timer.
     * I campi millis sono null quando l'orario non e' stato inserito.
     */
    public record WorkDayView(String entry, String lunchStart, String lunchEnd, String exit,
                              Long entryMillis, Long lunchStartMillis, Long lunchEndMillis, Long exitMillis) {

        static WorkDayView of(WorkDay day, ZoneId zone) {
            if (day == null) {
                return new WorkDayView("", "", "", "", null, null, null, null);
            }
            return new WorkDayView(
                    label(day.getEntryAt(), zone),
                    label(day.getLunchStartAt(), zone),
                    label(day.getLunchEndAt(), zone),
                    label(day.getExitAt(), zone),
                    millis(day.getEntryAt()),
                    millis(day.getLunchStartAt()),
                    millis(day.getLunchEndAt()),
                    millis(day.getExitAt()));
        }

        private static String label(Instant instant, ZoneId zone) {
            return instant == null ? "" : HHMM.format(instant.atZone(zone));
        }

        private static Long millis(Instant instant) {
            return instant == null ? null : instant.toEpochMilli();
        }

        public boolean started() {
            return entryMillis != null;
        }
    }
}
