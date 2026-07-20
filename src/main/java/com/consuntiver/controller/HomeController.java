package com.consuntiver.controller;

import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.ContextTimeService;
import com.consuntiver.service.FixedTaskService;
import com.consuntiver.service.TaskColorService;
import com.consuntiver.service.TaskLinkExtractor;
import com.consuntiver.service.UserConfigService;
import com.consuntiver.service.WorkDayService;
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
import java.util.HashMap;
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
    /** Solo data, per l'etichetta del pannello OGGI (es. 03/07). */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM", Locale.ITALY).withZone(ZONE);

    private final WorkEntryService workEntryService;
    private final WorkDayService workDayService;
    private final TaskLinkExtractor taskLinkExtractor;
    private final ContextTimeService contextTimeService;
    private final UserConfigService userConfigService;
    private final FixedTaskService fixedTaskService;
    private final TaskColorService taskColorService;

    public HomeController(WorkEntryService workEntryService,
                          WorkDayService workDayService,
                          TaskLinkExtractor taskLinkExtractor,
                          ContextTimeService contextTimeService,
                          UserConfigService userConfigService,
                          FixedTaskService fixedTaskService,
                          TaskColorService taskColorService) {
        this.workEntryService = workEntryService;
        this.workDayService = workDayService;
        this.taskLinkExtractor = taskLinkExtractor;
        this.contextTimeService = contextTimeService;
        this.userConfigService = userConfigService;
        this.fixedTaskService = fixedTaskService;
        this.taskColorService = taskColorService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String username = principal.getName();

        List<WorkEntry> entries = workEntryService.todayEntries(username, ZONE);
        WorkDayService.WorkDayView workDay = workDayService.today(username, ZONE);

        var config = userConfigService.get(username);
        ContextTimeService.Result contextTimes = contextTimeService.compute(entries, Instant.now());
        List<TaskLinkExtractor.TaskLink> taskLinks = taskLinkExtractor.extract(entries, config.getTaskBaseUrl());

        model.addAttribute("entries", entries);
        model.addAttribute("contextTimes", contextTimes.buttons());
        model.addAttribute("taskTimes", contextTimes.taskTotals());
        model.addAttribute("contextTotal", contextTimes);
        model.addAttribute("taskLinks", taskLinks);
        model.addAttribute("taskClipboard", buildTaskClipboard(taskLinks, entries, contextTimes));
        model.addAttribute("taskColorCss",
                taskColorService.buildCss(taskLinks.stream().map(TaskLinkExtractor.TaskLink::id).toList()));
        model.addAttribute("entryTaskClass", buildEntryTaskClass(entries));
        model.addAttribute("myTasks", fixedTaskService.options(username));
        model.addAttribute("homeUrl", config.getHomeUrl());
        model.addAttribute("workDay", workDay);
        model.addAttribute("targetSeconds", WorkDayService.TARGET_SECONDS);
        model.addAttribute("defaultBreakSeconds", WorkDayService.DEFAULT_BREAK_SECONDS);
        model.addAttribute("minBreakSeconds", WorkDayService.MIN_BREAK_SECONDS);
        model.addAttribute("serverNowMillis", System.currentTimeMillis());
        model.addAttribute("timeFormat", TIME_FORMAT);
        model.addAttribute("dateTimeFormat", DATE_TIME_FORMAT);
        model.addAttribute("todayLabel", DATE_FORMAT.format(Instant.now()));
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

    /** Mappa id-riga -> classe colore ({@code tc-<idtask>}) per le righe che citano un task. */
    private Map<Long, String> buildEntryTaskClass(List<WorkEntry> entries) {
        Map<Long, String> classes = new HashMap<>();
        for (WorkEntry entry : entries) {
            taskLinkExtractor.firstTaskId(entry.getDescription())
                    .ifPresent(id -> classes.put(entry.getId(), "tc-" + id));
        }
        return classes;
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

    @PostMapping("/entries/{id}/end")
    public String endActivity(@PathVariable("id") Long id, Principal principal) {
        workEntryService.endActivity(principal.getName(), id);
        return "redirect:/";
    }

    /** Modifica gli orari (inizio/fine, "HH:mm") di una voce. */
    @PostMapping("/entries/{id}/times")
    public String editTimes(@PathVariable("id") Long id,
                            @RequestParam(value = "start", required = false) String start,
                            @RequestParam(value = "end", required = false) String end,
                            Principal principal) {
        workEntryService.updateTimes(principal.getName(), id, start, end, ZONE);
        return "redirect:/";
    }

    @PostMapping("/entries/{id}/delete")
    public String delete(@PathVariable("id") Long id, Principal principal) {
        workEntryService.delete(principal.getName(), id);
        return "redirect:/";
    }

    /** Salva i quattro orari della giornata (entrata, pausa, rientro, uscita), inseriti a mano. */
    @PostMapping("/worktimes")
    public String saveWorkTimes(@RequestParam(value = "entry", required = false) String entry,
                                @RequestParam(value = "lunchStart", required = false) String lunchStart,
                                @RequestParam(value = "lunchEnd", required = false) String lunchEnd,
                                @RequestParam(value = "exit", required = false) String exit,
                                Principal principal) {
        workDayService.saveTimes(principal.getName(), ZONE, entry, lunchStart, lunchEnd, exit);
        return "redirect:/";
    }
}
