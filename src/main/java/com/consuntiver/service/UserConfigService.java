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
     * Configurazione dell'utente. Se non esiste NON viene applicato alcun default:
     * si restituisce una configurazione vuota (da compilare in Impostazioni), non
     * persistita finche' l'utente non salva.
     */
    public UserConfig get(String username) {
        User user = requireUser(username);
        return userConfigRepository.findByUser(user)
                .orElseGet(() -> new UserConfig(user, "", ""));
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
