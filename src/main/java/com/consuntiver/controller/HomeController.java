package com.consuntiver.controller;

import com.consuntiver.model.Attendance;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.AttendanceService;
import com.consuntiver.service.AttendanceService.WorkTimeSummary;
import com.consuntiver.service.WorkEntryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class HomeController {

    /** Fuso orario usato per delimitare "la giornata" e per mostrare gli orari. */
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ITALY).withZone(ZONE);

    /** Obiettivo giornaliero: 8 ore, in secondi. */
    private static final long TARGET_SECONDS = 8 * 60 * 60;

    private final WorkEntryService workEntryService;
    private final AttendanceService attendanceService;

    public HomeController(WorkEntryService workEntryService, AttendanceService attendanceService) {
        this.workEntryService = workEntryService;
        this.attendanceService = attendanceService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        String username = principal.getName();

        List<WorkEntry> entries = workEntryService.todayEntries(username, ZONE);
        List<Attendance> attendances = attendanceService.todayAttendances(username, ZONE);
        WorkTimeSummary summary = attendanceService.todaySummary(username, ZONE);

        model.addAttribute("entries", entries);
        model.addAttribute("attendances", attendances);
        model.addAttribute("summary", summary);
        model.addAttribute("targetSeconds", TARGET_SECONDS);
        model.addAttribute("serverNowMillis", System.currentTimeMillis());
        model.addAttribute("timeFormat", TIME_FORMAT);
        model.addAttribute("username", username);
        return "home";
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
}
