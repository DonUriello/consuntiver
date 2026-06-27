package com.consuntiver.controller;

import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.WorkEntryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final WorkEntryService workEntryService;

    public HomeController(WorkEntryService workEntryService) {
        this.workEntryService = workEntryService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        List<WorkEntry> entries = workEntryService.todayEntries(principal.getName(), ZONE);
        model.addAttribute("entries", entries);
        model.addAttribute("timeFormat", TIME_FORMAT);
        model.addAttribute("username", principal.getName());
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
}
