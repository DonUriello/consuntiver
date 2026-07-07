package com.consuntiver.controller;

import com.consuntiver.service.FixedTaskService;
import com.consuntiver.service.UserConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.Year;
import java.time.ZoneId;

@Controller
public class FixedTaskController {

    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    private final FixedTaskService fixedTaskService;
    private final UserConfigService userConfigService;

    public FixedTaskController(FixedTaskService fixedTaskService, UserConfigService userConfigService) {
        this.fixedTaskService = fixedTaskService;
        this.userConfigService = userConfigService;
    }

    @GetMapping("/fixed-tasks")
    public String page(Principal principal, Model model) {
        UserConfigService.ConfigView config = userConfigService.get(principal.getName());
        model.addAttribute("groups",
                fixedTaskService.listGroupedByYear(principal.getName(), config.getTaskBaseUrl()));
        model.addAttribute("currentYear", Year.now(ZONE).getValue());
        model.addAttribute("username", principal.getName());
        model.addAttribute("homeUrl", config.getHomeUrl());
        return "fixed-tasks";
    }

    @PostMapping("/fixed-tasks")
    public String add(@RequestParam(value = "taskNumber", required = false) String taskNumber,
                      @RequestParam(value = "name", required = false) String name,
                      @RequestParam(value = "description", required = false) String description,
                      @RequestParam("year") int year,
                      Principal principal) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasNumber = taskNumber != null && !taskNumber.isBlank();
        if (hasName || hasNumber) {
            fixedTaskService.add(principal.getName(), taskNumber, name, description, year);
        }
        return "redirect:/fixed-tasks";
    }

    @PostMapping("/fixed-tasks/{id}/update")
    public String update(@PathVariable("id") Long id,
                         @RequestParam(value = "taskNumber", required = false) String taskNumber,
                         @RequestParam(value = "name", required = false) String name,
                         @RequestParam(value = "description", required = false) String description,
                         @RequestParam("year") int year,
                         Principal principal) {
        boolean hasName = name != null && !name.isBlank();
        boolean hasNumber = taskNumber != null && !taskNumber.isBlank();
        if (hasName || hasNumber) {
            fixedTaskService.update(principal.getName(), id, taskNumber, name, description, year);
        }
        return "redirect:/fixed-tasks";
    }

    @PostMapping("/fixed-tasks/{id}/delete")
    public String delete(@PathVariable("id") Long id, Principal principal) {
        fixedTaskService.delete(principal.getName(), id);
        return "redirect:/fixed-tasks";
    }
}
