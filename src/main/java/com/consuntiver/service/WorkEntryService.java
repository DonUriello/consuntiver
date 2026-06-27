package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkEntryRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class WorkEntryService {

    private final WorkEntryRepository workEntryRepository;
    private final UserRepository userRepository;

    public WorkEntryService(WorkEntryRepository workEntryRepository, UserRepository userRepository) {
        this.workEntryRepository = workEntryRepository;
        this.userRepository = userRepository;
    }

    /** Salva una nuova voce per l'utente con l'ora corrente. */
    public WorkEntry add(String username, String description) {
        User user = requireUser(username);
        WorkEntry entry = new WorkEntry(Instant.now(), description.trim(), user);
        return workEntryRepository.save(entry);
    }

    /** Voci di oggi per l'utente, dalla piu' recente alla piu' vecchia. */
    public List<WorkEntry> todayEntries(String username, ZoneId zone) {
        User user = requireUser(username);
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        return workEntryRepository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, from, to);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }
}
