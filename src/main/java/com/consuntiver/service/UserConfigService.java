package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.model.UserConfig;
import com.consuntiver.repository.UserConfigRepository;
import com.consuntiver.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserConfigService {

    private final UserConfigRepository userConfigRepository;
    private final UserRepository userRepository;

    public UserConfigService(UserConfigRepository userConfigRepository, UserRepository userRepository) {
        this.userConfigRepository = userConfigRepository;
        this.userRepository = userRepository;
    }

    /**
     * Configurazione dell'utente; se non esiste viene creata con i valori di default
     * (gli stessi attualmente in uso).
     */
    public UserConfig get(String username) {
        User user = requireUser(username);
        return userConfigRepository.findByUser(user)
                .orElseGet(() -> userConfigRepository.save(
                        new UserConfig(user, EasyLinks.DEFAULT_HOME_URL, EasyLinks.DEFAULT_ISSUE_BASE_URL)));
    }

    /** Aggiorna gli indirizzi della configurazione dell'utente. */
    public UserConfig update(String username, String homeUrl, String taskBaseUrl) {
        UserConfig config = get(username);
        config.setHomeUrl(homeUrl.trim());
        config.setTaskBaseUrl(taskBaseUrl.trim());
        return userConfigRepository.save(config);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }
}
