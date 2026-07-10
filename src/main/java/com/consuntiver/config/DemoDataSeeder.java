package com.consuntiver.config;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkDay;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkDayRepository;
import com.consuntiver.repository.WorkEntryRepository;
import com.consuntiver.service.EasyLinks;
import com.consuntiver.service.UserConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seeder per lo sviluppo locale: all'avvio crea (se manca) l'utente demo e ne rigenera una
 * giornata di lavoro simulata, ancorata ad "adesso". In produzione (profilo prod, Supabase)
 * NON viene eseguito: lì la demo si inizializza via script SQL.
 */
@Component
@Profile("!prod")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_USERNAME = "demo_galileo";
    private static final String DEMO_PASSWORD = "death_earth";
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    private final UserRepository userRepository;
    private final UserConfigService userConfigService;
    private final WorkEntryRepository workEntryRepository;
    private final WorkDayRepository workDayRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          UserConfigService userConfigService,
                          WorkEntryRepository workEntryRepository,
                          WorkDayRepository workDayRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userConfigService = userConfigService;
        this.workEntryRepository = workEntryRepository;
        this.workDayRepository = workDayRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Un segmento della giornata simulata: durata in minuti e descrizione. */
    private record Segment(int minutes, String description) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User demo = userRepository.findByUsername(DEMO_USERNAME).orElseGet(() ->
                userRepository.save(new User(DEMO_USERNAME, passwordEncoder.encode(DEMO_PASSWORD))));

        // Configurazione demo (URL Galileo di default).
        userConfigService.update(DEMO_USERNAME, EasyLinks.DEFAULT_HOME_URL, EasyLinks.DEFAULT_ISSUE_BASE_URL);

        // Rigenera ogni avvio, cosi' la demo mostra sempre una giornata "di oggi".
        workEntryRepository.deleteByUser(demo);
        workDayRepository.deleteByUser(demo);

        // Giornata intera 08:00 -> 17:00 (di oggi, fuso Roma), tutte le righe chiuse.
        List<Segment> day = List.of(
                new Segment(60, "#129671 analisi requisiti"),        // 08:00 - 09:00
                new Segment(45, "#119971 fix bug login"),            // 09:00 - 09:45
                new Segment(75, "#129671 sviluppo nuova feature"),   // 09:45 - 11:00
                new Segment(40, "#119995 review della pull request"),// 11:00 - 11:40
                new Segment(50, "#119971 riunione di team"),         // 11:40 - 12:30
                new Segment(30, "#129671 correzioni post review"),   // 12:30 - 13:00
                new Segment(60, "Pausa pranzo"),                     // 13:00 - 14:00
                new Segment(60, "#119995 stesura documentazione"),   // 14:00 - 15:00
                new Segment(45, "#119971 refactoring"),              // 15:00 - 15:45
                new Segment(35, "#119995 deploy in staging"),        // 15:45 - 16:20
                new Segment(40, "#129671 aggiornamento ticket")      // 16:20 - 17:00
        );

        Instant cursor = LocalDate.now(ZONE).atTime(8, 0).atZone(ZONE).toInstant();
        for (Segment seg : day) {
            Instant start = cursor;
            Instant end = start.plus(seg.minutes(), ChronoUnit.MINUTES);
            WorkEntry entry = new WorkEntry(start, seg.description(), demo);
            entry.setEndedAt(end);
            workEntryRepository.save(entry);
            cursor = end;
        }

        // Orari giornata: entrata 08:00, pausa 13:00 -> 14:00, uscita 17:00 (8 ore lavorate).
        LocalDate today = LocalDate.now(ZONE);
        WorkDay workDay = new WorkDay(today, demo);
        workDay.setEntryAt(today.atTime(8, 0).atZone(ZONE).toInstant());
        workDay.setLunchStartAt(today.atTime(13, 0).atZone(ZONE).toInstant());
        workDay.setLunchEndAt(today.atTime(14, 0).atZone(ZONE).toInstant());
        workDay.setExitAt(today.atTime(17, 0).atZone(ZONE).toInstant());
        workDayRepository.save(workDay);
    }
}
