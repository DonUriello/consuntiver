package com.consuntiver.controller;

import com.consuntiver.model.Attendance;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.AttendanceService;
import com.consuntiver.service.AttendanceService.WorkTimeSummary;
import com.consuntiver.service.ContextTimeService;
import com.consuntiver.service.TaskLinkExtractor;
import com.consuntiver.service.UserConfigService;
import com.consuntiver.service.WorkEntryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    /** Fuso orario usato per delimitare "la giornata" e per mostrare gli orari. */
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY).withZone(ZONE);
    /** Data e ora di inizio riga (es. 01/07 09:30). */
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.ITALY).withZone(ZONE);

    /** Obiettivo giornaliero: 8 ore, in secondi. */
    private static final long TARGET_SECONDS = 8 * 60 * 60;

    private final WorkEntryService workEntryService;
    private final AttendanceService attendanceService;
    private final TaskLinkExtractor taskLinkExtractor;
    private final ContextTimeService contextTimeService;
    private final UserConfigService userConfigService;

    public HomeController(WorkEntryService workEntryService,
                          AttendanceService attendanceService,
                          TaskLinkExtractor taskLinkExtractor,
                          ContextTimeService contextTimeService,
                          UserConfigService userConfigService) {
        this.workEntryService = workEntryService;
        this.attendanceService = attendanceService;
        this.taskLinkExtractor = taskLinkExtractor;
        this.contextTimeService = contextTimeService;
        this.userConfigService = userConfigService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String username = principal.getName();

        List<WorkEntry> entries = workEntryService.todayEntries(username, ZONE);
        List<Attendance> attendances = attendanceService.todayAttendances(username, ZONE);
        WorkTimeSummary summary = attendanceService.todaySummary(username, ZONE);

        var config = userConfigService.get(username);
        ContextTimeService.Result contextTimes = contextTimeService.compute(entries, Instant.now());
        List<TaskLinkExtractor.TaskLink> taskLinks = taskLinkExtractor.extract(entries, config.getTaskBaseUrl());

        model.addAttribute("entries", entries);
        model.addAttribute("contextTimes", contextTimes.buttons());
        model.addAttribute("taskTimes", contextTimes.taskTotals());
        model.addAttribute("contextTotal", contextTimes);
        model.addAttribute("taskLinks", taskLinks);
        model.addAttribute("taskClipboard", buildTaskClipboard(taskLinks, entries, contextTimes));
        model.addAttribute("homeUrl", config.getHomeUrl());
        model.addAttribute("attendances", attendances);
        model.addAttribute("summary", summary);
        model.addAttribute("targetSeconds", TARGET_SECONDS);
        model.addAttribute("serverNowMillis", System.currentTimeMillis());
        model.addAttribute("timeFormat", TIME_FORMAT);
        model.addAttribute("dateTimeFormat", DATE_TIME_FORMAT);
        model.addAttribute("username", username);
        return "home";
    }

    /**
     * Per ogni task costruisce il testo da copiare negli appunti:
     * {@code <tempo dedicato> - <descrizioni delle attivita' separate da virgola>}.
     */
    private Map<String, String> buildTaskClipboard(List<TaskLinkExtractor.TaskLink> taskLinks,
                                                   List<WorkEntry> entries,
                                                   ContextTimeService.Result contextTimes) {
        // Righe in ordine cronologico (entries arriva dalla piu' recente alla piu' vecchia).
        List<WorkEntry> ascending = new ArrayList<>(entries);
        Collections.reverse(ascending);

        Map<String, String> clipboard = new LinkedHashMap<>();
        for (TaskLinkExtractor.TaskLink task : taskLinks) {
            String descriptions = ascending.stream()
                    .filter(e -> task.id().equals(taskLinkExtractor.firstTaskId(e.getDescription()).orElse(null)))
                    .map(e -> stripTaskToken(e.getDescription(), task.id()))
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));

            String time = contextTimes.taskTotals().containsKey(task.id())
                    ? contextTimes.taskTotals().get(task.id()).label().replace("+", "")
                    : "";
            clipboard.put(task.id(), time + " - " + descriptions);
        }
        return clipboard;
    }

    /** Rimuove il riferimento al task (es. "#129671") dalla descrizione, lasciando l'attivita'. */
    private static String stripTaskToken(String description, String taskId) {
        return description
                .replace("#" + taskId, "")
                .replaceAll("(?<![0-9])" + taskId + "(?![0-9])", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @PostMapping("/entries")
    public String add(@RequestParam("description") @NotBlank String description,
                      Principal principal) {
        if (description != null && !description.isBlank()) {
            workEntryService.add(principal.getName(), description);
        }
        return "redirect:/";
    }

    @PostMapping("/entries/{id}")
    public String edit(@PathVariable("id") Long id,
                       @RequestParam("description") String description,
                       Principal principal) {
        if (description != null && !description.isBlank()) {
            workEntryService.updateDescription(principal.getName(), id, description);
        }
        return "redirect:/";
    }

    @PostMapping("/attendance/in")
    public String clockIn(Principal principal) {
        attendanceService.clockIn(principal.getName());
        return "redirect:/";
    }

    @PostMapping("/attendance/out")
    public String clockOut(Principal principal) {
        attendanceService.clockOut(principal.getName());
        return "redirect:/";
    }

    /** Come la pausa (chiude la sessione) ma segna anche "Fine giornata" nello storico. */
    @PostMapping("/attendance/end")
    public String endDay(Principal principal) {
        String username = principal.getName();
        attendanceService.clockOut(username);
        workEntryService.add(username, "Fine giornata");
        return "redirect:/";
    }
}
