package com.consuntiver.controller;

import com.consuntiver.service.UserService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Validated @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            userService.register(form.getUsername().trim(), form.getPassword(), form.getEmail());
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("username", "duplicate", ex.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    public static class RegisterForm {

        @NotBlank(message = "Lo username e' obbligatorio")
        @Size(min = 3, max = 50, message = "Lo username deve avere tra 3 e 50 caratteri")
        private String username;

        @NotBlank(message = "La password e' obbligatoria")
        @Size(min = 6, max = 100, message = "La password deve avere almeno 6 caratteri")
        private String password;

        /** Facoltativa. */
        @Email(message = "Email non valida")
        private String email;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
