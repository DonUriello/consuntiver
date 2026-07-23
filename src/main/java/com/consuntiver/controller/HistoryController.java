package com.consuntiver.controller;

import com.consuntiver.service.HistoryService;
import com.consuntiver.service.UserConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Controller
public class HistoryController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    private final HistoryService historyService;
    private final UserConfigService userConfigService;

    public HistoryController(HistoryService historyService, UserConfigService userConfigService) {
        this.historyService = historyService;
        this.userConfigService = userConfigService;
    }

    /**
     * Storico a calendario. {@code month} (YYYY-MM) sceglie il mese mostrato;
     * {@code day} (YYYY-MM-DD) seleziona un giorno e ne mostra il consuntivo sotto.
     * Valori mancanti o non validi ricadono sul mese corrente / nessun giorno.
     */
    @GetMapping("/storico")
    public String page(@RequestParam(value = "month", required = false) String month,
                       @RequestParam(value = "day", required = false) String day,
                       Principal principal, Model model) {
        String username = principal.getName();

        LocalDate selected = parseDate(day);
        YearMonth ym = parseMonth(month);
        if (ym == null) {
            ym = (selected != null) ? YearMonth.from(selected) : YearMonth.now(ZONE);
        }

        model.addAttribute("calendar", historyService.calendar(username, ZONE, ym, selected));

        if (selected != null) {
            HistoryService.DayDetail detail = historyService.day(username, ZONE, selected);
            model.addAttribute("dayDetail", detail);
            model.addAttribute("taskColorCss", detail.taskColorCss());
        } else {
            model.addAttribute("taskColorCss", "");
        }

        model.addAttribute("username", username);
        model.addAttribute("homeUrl", userConfigService.get(username).getHomeUrl());
        return "history";
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
