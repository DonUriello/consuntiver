package com.consuntiver.controller;

import com.consuntiver.service.HistoryService;
import com.consuntiver.service.UserConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
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

    @GetMapping("/storico")
    public String page(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("history", historyService.history(username, ZONE));
        model.addAttribute("username", username);
        model.addAttribute("homeUrl", userConfigService.get(username).getHomeUrl());
        return "history";
    }
}
