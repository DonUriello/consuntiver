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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Costruisce lo storico delle giornate passate: per ogni giorno (escluso oggi, gia'
 * visibile in home) raccoglie orari, lavoro netto, totale del tempo, ripartizione per
 * task e l'elenco delle attivita'. Riusa {@link ContextTimeService} per i totali e
 * {@link TaskColorService} per i colori dei task.
 */
@Service
public class HistoryService {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY);
    /** Es. "mar 08/07" (giorno della settimana + data). */
    private static final DateTimeFormatter DAY_LABEL =
            DateTimeFormatter.ofPattern("EEE dd/MM", Locale.ITALY);

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

    public HistoryView history(String username, ZoneId zone) {
        User user = requireUser(username);
        String taskBaseUrl = userConfigService.get(username).getTaskBaseUrl();
        LocalDate today = LocalDate.now(zone);

        // Attivita' raggruppate per giornata (fuso Europe/Rome).
        Map<LocalDate, List<WorkEntry>> entriesByDate = new HashMap<>();
        for (WorkEntry entry : workEntryRepository.findByUserOrderByStartedAtDesc(user)) {
            LocalDate date = entry.getStartedAt().atZone(zone).toLocalDate();
            entriesByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(entry);
        }

        // Orari (entrata/pausa/...) per giornata.
        Map<LocalDate, WorkDay> daysByDate = new HashMap<>();
        for (WorkDay day : workDayRepository.findByUserOrderByWorkDateDesc(user)) {
            daysByDate.put(day.getWorkDate(), day);
        }

        // Unione delle date (attivita' + orari), escluso oggi, dalla piu' recente.
        TreeSet<LocalDate> dates = new TreeSet<>(Comparator.reverseOrder());
        dates.addAll(entriesByDate.keySet());
        dates.addAll(daysByDate.keySet());
        dates.remove(today);

        List<DayView> days = new ArrayList<>();
        List<String> allTaskIds = new ArrayList<>();
        for (LocalDate date : dates) {
            List<WorkEntry> dayEntries = entriesByDate.getOrDefault(date, List.of());
            // Le righe eventualmente ancora aperte contano fino a fine giornata.
            Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
            ContextTimeService.Result totals = contextTimeService.compute(dayEntries, dayEnd);
            List<TaskLinkExtractor.TaskLink> links = taskLinkExtractor.extract(dayEntries, taskBaseUrl);

            List<TaskBreakdown> tasks = new ArrayList<>();
            for (TaskLinkExtractor.TaskLink link : links) {
                allTaskIds.add(link.id());
                ContextTimeService.ContextButton b = totals.taskTotals().get(link.id());
                tasks.add(new TaskBreakdown("tc-" + link.id(), link.id(), link.url(),
                        b != null ? b.label() : "", b != null ? b.actual() : ""));
            }

            List<EntryRow> rows = new ArrayList<>();
            for (WorkEntry e : dayEntries) {
                String end = e.getEndedAt() != null ? TIME.format(e.getEndedAt().atZone(zone)) : "…";
                String colorClass = taskLinkExtractor.firstTaskId(e.getDescription())
                        .map(id -> "tc-" + id).orElse("");
                rows.add(new EntryRow(TIME.format(e.getStartedAt().atZone(zone)) + " → " + end,
                        e.getDescription(), colorClass));
            }

            WorkDay wd = daysByDate.get(date);
            days.add(new DayView(
                    DAY_LABEL.format(date),
                    time(wd == null ? null : wd.getEntryAt(), zone),
                    time(wd == null ? null : wd.getLunchStartAt(), zone),
                    time(wd == null ? null : wd.getLunchEndAt(), zone),
                    time(wd == null ? null : wd.getExitAt(), zone),
                    formatSeconds(WorkDayService.netWorkedSeconds(wd)),
                    totals.totalLabel(),
                    totals.totalActual(),
                    totals.hasTotal(),
                    tasks,
                    rows));
        }

        return new HistoryView(days, taskColorService.buildCss(allTaskIds));
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
        return (minutes / 60) + ":" + String.format(Locale.ITALY, "%02d", minutes % 60);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /** Storico completo: le giornate e il CSS dei colori dei task. */
    public record HistoryView(List<DayView> days, String taskColorCss) {
        public boolean isEmpty() {
            return days.isEmpty();
        }
    }

    /**
     * Una giornata dello storico.
     *
     * @param netWorked lavoro netto "h:mm" (pausa esclusa), o "—" se manca entrata/uscita
     * @param totalLabel totale del tempo sulle attivita' in quarti d'ora (es. 6,5)
     */
    public record DayView(String dateLabel, String entry, String lunchStart, String lunchEnd, String exit,
                          String netWorked, String totalLabel, String totalActual, boolean hasTotal,
                          List<TaskBreakdown> tasks, List<EntryRow> entries) {
        public boolean hasTimes() {
            return !"—".equals(entry) || !"—".equals(exit);
        }
    }

    /** Ripartizione del tempo per un task nella giornata. */
    public record TaskBreakdown(String colorClass, String id, String url, String label, String actual) {
    }

    /** Una riga di attivita' nello storico. */
    public record EntryRow(String time, String description, String colorClass) {
    }
}
