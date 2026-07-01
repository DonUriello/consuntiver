package com.consuntiver.config;

import com.consuntiver.model.Attendance;
import com.consuntiver.model.User;
import com.consuntiver.model.UserConfig;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.AttendanceRepository;
import com.consuntiver.repository.UserConfigRepository;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkEntryRepository;
import com.consuntiver.service.EasyLinks;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * All'avvio crea (se manca) l'utente demo e ne rigenera una giornata di lavoro simulata,
 * ancorata ad "adesso" cosi' che sia sempre visibile nella vista di oggi.
 */
@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_USERNAME = "demo_galileo";
    private static final String DEMO_PASSWORD = "death_earth";

    private final UserRepository userRepository;
    private final UserConfigRepository userConfigRepository;
    private final WorkEntryRepository workEntryRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          UserConfigRepository userConfigRepository,
                          WorkEntryRepository workEntryRepository,
                          AttendanceRepository attendanceRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userConfigRepository = userConfigRepository;
        this.workEntryRepository = workEntryRepository;
        this.attendanceRepository = attendanceRepository;
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

        if (userConfigRepository.findByUser(demo).isEmpty()) {
            userConfigRepository.save(new UserConfig(demo,
                    EasyLinks.DEFAULT_HOME_URL, EasyLinks.DEFAULT_ISSUE_BASE_URL));
        }

        // Rigenera ogni avvio, cosi' la demo mostra sempre una giornata "di oggi".
        workEntryRepository.deleteByUser(demo);
        attendanceRepository.deleteByUser(demo);

        // 5 task d'esempio (alcuni ripetuti) + una pausa, per mostrare l'accumulo del tempo.
        List<Segment> day = List.of(
                new Segment(40, "#123456 analisi requisiti"),
                new Segment(35, "#234567 fix bug login"),
                new Segment(50, "#123456 sviluppo nuova feature"),
                new Segment(45, "Pausa pranzo"),
                new Segment(30, "#345678 review della pull request"),
                new Segment(25, "#456789 riunione di team"),
                new Segment(20, "#234567 test del fix"),
                new Segment(30, "#567890 stesura documentazione"),
                new Segment(15, "#345678 deploy in staging")   // ultima: in corso
        );

        int totalMinutes = day.stream().mapToInt(Segment::minutes).sum();
        Instant now = Instant.now();
        Instant cursor = now.minus(totalMinutes, ChronoUnit.MINUTES);

        for (int i = 0; i < day.size(); i++) {
            Segment seg = day.get(i);
            Instant start = cursor;
            boolean last = (i == day.size() - 1);
            WorkEntry entry = new WorkEntry(start, seg.description(), demo);
            if (!last) {
                entry.setEndedAt(start.plus(seg.minutes(), ChronoUnit.MINUTES));
            }
            workEntryRepository.save(entry);
            cursor = start.plus(seg.minutes(), ChronoUnit.MINUTES);
        }

        // Timbrature: mattina chiusa prima della pausa, pomeriggio ancora in servizio.
        Instant dayStart = now.minus(totalMinutes, ChronoUnit.MINUTES);
        Attendance morning = new Attendance(dayStart, demo);
        morning.setClockOut(dayStart.plus(40 + 35 + 50, ChronoUnit.MINUTES)); // fino alla pausa
        attendanceRepository.save(morning);

        Attendance afternoon = new Attendance(
                dayStart.plus(40 + 35 + 50 + 45, ChronoUnit.MINUTES), demo); // dopo la pausa, aperta
        attendanceRepository.save(afternoon);
    }
}
