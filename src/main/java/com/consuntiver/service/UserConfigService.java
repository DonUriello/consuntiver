package com.consuntiver.service;

import com.consuntiver.model.Settings;
import com.consuntiver.model.User;
import com.consuntiver.repository.SettingsRepository;
import com.consuntiver.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Gestisce la configurazione per-utente (memorizzata come impostazioni chiave/valore). */
@Service
public class UserConfigService {

    public static final String HOME_URL = "home_url";
    public static final String TASK_BASE_URL = "task_base_url";

    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;

    public UserConfigService(SettingsRepository settingsRepository, UserRepository userRepository) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
    }

    /**
     * Configurazione dell'utente. Se una chiave manca il valore e' vuoto (nessun default):
     * va compilata in Impostazioni.
     */
    public ConfigView get(String username) {
        User user = requireUser(username);
        List<Settings> settings = settingsRepository.findByUser(user);
        return new ConfigView(valueOf(settings, HOME_URL), valueOf(settings, TASK_BASE_URL));
    }

    /** Salva (upsert) gli indirizzi della configurazione dell'utente. */
    public void update(String username, String homeUrl, String taskBaseUrl) {
        User user = requireUser(username);
        upsert(user, HOME_URL, homeUrl.trim());
        upsert(user, TASK_BASE_URL, taskBaseUrl.trim());
    }

    private void upsert(User user, String code, String value) {
        Settings s = settingsRepository.findByUserAndCode(user, code)
                .orElseGet(() -> new Settings(user, code, value));
        s.setValue(value);
        settingsRepository.save(s);
    }

    private String valueOf(List<Settings> settings, String code) {
        return settings.stream()
                .filter(s -> s.getCode().equals(code))
                .map(Settings::getValue)
                .findFirst()
                .orElse("");
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /** Vista di sola lettura della configurazione, usata da controller e template. */
    public static class ConfigView {
        private final String homeUrl;
        private final String taskBaseUrl;

        public ConfigView(String homeUrl, String taskBaseUrl) {
            this.homeUrl = homeUrl;
            this.taskBaseUrl = taskBaseUrl;
        }

        public String getHomeUrl() {
            return homeUrl;
        }

        public String getTaskBaseUrl() {
            return taskBaseUrl;
        }
    }
}
