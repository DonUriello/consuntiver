package com.consuntiver.controller;

import com.consuntiver.service.FixedTaskService;
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

    public FixedTaskController(FixedTaskService fixedTaskService) {
        this.fixedTaskService = fixedTaskService;
    }

    @GetMapping("/fixed-tasks")
    public String page(Principal principal, Model model) {
        model.addAttribute("groups", fixedTaskService.listGroupedByYear(principal.getName()));
        model.addAttribute("currentYear", Year.now(ZONE).getValue());
        model.addAttribute("username", principal.getName());
        return "fixed-tasks";
    }

    @PostMapping("/fixed-tasks")
    public String add(@RequestParam(value = "taskNumber", required = false) String taskNumber,
                      @RequestParam("description") String description,
                      @RequestParam("year") int year,
                      Principal principal) {
        if (description != null && !description.isBlank()) {
            fixedTaskService.add(principal.getName(), taskNumber, description, year);
        }
        return "redirect:/fixed-tasks";
    }

    @PostMapping("/fixed-tasks/{id}/delete")
    public String delete(@PathVariable("id") Long id, Principal principal) {
        fixedTaskService.delete(principal.getName(), id);
        return "redirect:/fixed-tasks";
    }
}
