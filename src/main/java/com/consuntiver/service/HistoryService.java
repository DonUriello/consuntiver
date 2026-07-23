package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkDay;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkDayRepository;
import com.consuntiver.repository.WorkEntryRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Storico delle giornate passate mostrato come <b>calendario mensile</b>: si sceglie
 * un giorno e sotto compare il consuntivo compatto di quella giornata, una riga per
 * task (numero del task con link, concatenazione delle attivita', tempo totale).
 * Riusa {@link ContextTimeService} per i totali e {@link TaskColorService} per i colori.
 */
@Service
public class HistoryService {

    private static final Locale IT = Locale.ITALY;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", IT);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", IT);
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("EEEE d MMMM", IT);
    private static final List<String> WEEKDAYS = List.of("lun", "mar", "mer", "gio", "ven", "sab", "dom");

    private final WorkEntryRepository workEntryRepository;
    private final WorkDayRepository workDayRepository;
    private final UserRepository userRepository;
    private final UserConfigService userConfigService;
    private final ContextTimeService contextTimeService;
    private final TaskLinkExtractor taskLinkExtractor;
    private final TaskColorService taskColorService;

    public HistoryService(WorkEntryRepository workEntryRepository,
                          WorkDayRepository workDayRepository,
                          UserRepository userRepository,
                          UserConfigService userConfigService,
                          ContextTimeService contextTimeService,
                          TaskLinkExtractor taskLinkExtractor,
                          TaskColorService taskColorService) {
        this.workEntryRepository = workEntryRepository;
        this.workDayRepository = workDayRepository;
        this.userRepository = userRepository;
        this.userConfigService = userConfigService;
        this.contextTimeService = contextTimeService;
        this.taskLinkExtractor = taskLinkExtractor;
        this.taskColorService = taskColorService;
    }

    /** Griglia del mese {@code ym}, con i giorni che hanno dati resi cliccabili. */
    public CalendarView calendar(String username, ZoneId zone, YearMonth ym, LocalDate selected) {
        User user = requireUser(username);
        Set<LocalDate> withData = datesWithData(user, zone);
        LocalDate today = LocalDate.now(zone);

        LocalDate first = ym.atDay(1);
        int shift = (first.getDayOfWeek().getValue() + 6) % 7; // lunedi' = 0
        LocalDate gridStart = first.minusDays(shift);

        List<CalWeek> weeks = new ArrayList<>();
        List<CalDay> current = new ArrayList<>();
        for (int i = 0; i < 42; i++) { // 6 settimane, coprono sempre il mese
            LocalDate d = gridStart.plusDays(i);
            boolean inMonth = YearMonth.from(d).equals(ym);
            current.add(new CalDay(
                    d.getDayOfMonth(), d.toString(), inMonth,
                    inMonth && withData.contains(d), d.equals(today), d.equals(selected)));
            if (current.size() == 7) {
                weeks.add(new CalWeek(current));
                current = new ArrayList<>();
            }
        }
        // Elimina eventuali settimane finali completamente fuori dal mese.
        while (weeks.size() > 4 && weeks.get(weeks.size() - 1).days().stream().noneMatch(CalDay::inMonth)) {
            weeks.remove(weeks.size() - 1);
        }

        return new CalendarView(
                capitalize(ym.format(MONTH_LABEL)),
                ym.minusMonths(1).toString(),
                ym.plusMonths(1).toString(),
                WEEKDAYS, weeks);
    }

    /** Consuntivo compatto di una giornata: una riga per task. */
    public DayDetail day(String username, ZoneId zone, LocalDate date) {
        User user = requireUser(username);
        String taskBaseUrl = userConfigService.get(username).getTaskBaseUrl();

        Instant from = date.atStartOfDay(zone).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<WorkEntry> dayEntries =
                workEntryRepository.findByUserAndStartedAtBetweenOrderByStartedAtDesc(user, from, to);

        ContextTimeService.Result totals = contextTimeService.compute(dayEntries, to);
        List<TaskLinkExtractor.TaskLink> links = taskLinkExtractor.extract(dayEntries, taskBaseUrl);

        // Ordine cronologico per concatenare le attivita' dalla piu' vecchia.
        List<WorkEntry> ascending = new ArrayList<>(dayEntries);
        Collections.reverse(ascending);

        List<TaskRow> tasks = new ArrayList<>();
        List<String> taskIds = new ArrayList<>();
        for (TaskLinkExtractor.TaskLink link : links) {
            taskIds.add(link.id());
            String comments = ascending.stream()
                    .filter(e -> link.id().equals(taskLinkExtractor.firstTaskId(e.getDescription()).orElse(null)))
                    .map(e -> stripTaskToken(e.getDescription(), link.id()))
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            ContextTimeService.ContextButton b = totals.taskTotals().get(link.id());
            String label = b != null ? b.label() : "";
            String actual = b != null ? b.actual() : "";
            // Clipboard come in home: "<tempo totale> - <attivita' concatenate>".
            String clipboard = label.replace("+", "") + " - " + comments;
            tasks.add(new TaskRow("tc-" + link.id(), "#" + link.id(), link.url(), comments, label, actual, clipboard));
        }

        WorkDay wd = workDayRepository.findByUserAndWorkDate(user, date).orElse(null);
        boolean hasTimes = wd != null && (wd.getEntryAt() != null || wd.getExitAt() != null);
        return new DayDetail(
                capitalize(FULL_DAY.format(date)),
                time(wd == null ? null : wd.getEntryAt(), zone),
                time(wd == null ? null : wd.getLunchStartAt(), zone),
                time(wd == null ? null : wd.getLunchEndAt(), zone),
                time(wd == null ? null : wd.getExitAt(), zone),
                formatSeconds(WorkDayService.netWorkedSeconds(wd)),
                hasTimes,
                totals.totalLabel(), totals.totalActual(), totals.hasTotal(),
                tasks, taskColorService.buildCss(taskIds));
    }

    /** Insieme delle date (fuso indicato) che hanno attivita' o orari registrati. */
    private Set<LocalDate> datesWithData(User user, ZoneId zone) {
        Set<LocalDate> dates = new HashSet<>();
        for (WorkEntry e : workEntryRepository.findByUserOrderByStartedAtDesc(user)) {
            dates.add(e.getStartedAt().atZone(zone).toLocalDate());
        }
        for (WorkDay wd : workDayRepository.findByUserOrderByWorkDateDesc(user)) {
            dates.add(wd.getWorkDate());
        }
        return dates;
    }

    /** Rimuove il riferimento al task (es. "#129671") dalla descrizione, lasciando l'attivita'. */
    private static String stripTaskToken(String description, String taskId) {
        return description
                .replace("#" + taskId, "")
                .replaceAll("(?<![0-9])" + taskId + "(?![0-9])", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String time(Instant instant, ZoneId zone) {
        return instant == null ? "—" : TIME.format(instant.atZone(zone));
    }

    /** Secondi -> "h:mm", oppure "—" se null. */
    private String formatSeconds(Long seconds) {
        if (seconds == null) {
            return "—";
        }
        long minutes = Math.round(seconds / 60.0);
        return (minutes / 60) + ":" + String.format(IT, "%02d", minutes % 60);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /** Il calendario del mese. */
    public record CalendarView(String monthLabel, String prevMonth, String nextMonth,
                               List<String> weekdayLabels, List<CalWeek> weeks) {
    }

    /** Una settimana (7 celle) del calendario. */
    public record CalWeek(List<CalDay> days) {
    }

    /** Una cella-giorno del calendario. */
    public record CalDay(int day, String iso, boolean inMonth, boolean hasData,
                         boolean today, boolean selected) {
    }

    /** Consuntivo di una giornata selezionata. */
    public record DayDetail(String dateLabel, String entry, String lunchStart, String lunchEnd, String exit,
                            String netWorked, boolean hasTimes,
                            String totalLabel, String totalActual, boolean hasTotal,
                            List<TaskRow> tasks, String taskColorCss) {
    }

    /**
     * Una riga del consuntivo: un task con le sue attivita' concatenate e il tempo.
     *
     * @param clipboard testo copiato al click sul link ("<tempo> - <attivita'>")
     */
    public record TaskRow(String colorClass, String code, String url, String comments,
                          String totalLabel, String totalActual, String clipboard) {
    }
}
