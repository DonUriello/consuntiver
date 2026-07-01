package com.consuntiver.controller;

import com.consuntiver.model.UserConfig;
import com.consuntiver.service.UserConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class SettingsController {

    private final UserConfigService userConfigService;

    public SettingsController(UserConfigService userConfigService) {
        this.userConfigService = userConfigService;
    }

    @GetMapping("/settings")
    public String page(Principal principal, Model model) {
        UserConfig config = userConfigService.get(principal.getName());
        model.addAttribute("config", config);
        model.addAttribute("homeUrl", config.getHomeUrl());
        model.addAttribute("username", principal.getName());
        return "settings";
    }

    @PostMapping("/settings")
    public String save(@RequestParam("homeUrl") String homeUrl,
                       @RequestParam("taskBaseUrl") String taskBaseUrl,
                       Principal principal) {
        if (!homeUrl.isBlank() && !taskBaseUrl.isBlank()) {
            userConfigService.update(principal.getName(), homeUrl, taskBaseUrl);
        }
        return "redirect:/settings?saved";
    }
}
