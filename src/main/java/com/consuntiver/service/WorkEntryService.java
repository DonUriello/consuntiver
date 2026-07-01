package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkEntryRepository;
import org.springframework.security.access.AccessDeniedException;
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

    /**
     * Salva una nuova voce con l'ora corrente come inizio. All'inserimento della nuova riga
     * la voce precedente ancora aperta viene chiusa: la sua fine diventa "adesso".
     */
    public WorkEntry add(String username, String description) {
        User user = requireUser(username);
        Instant now = Instant.now();
        workEntryRepository.findFirstByUserAndEndedAtIsNullOrderByStartedAtDesc(user)
                .ifPresent(open -> {
                    open.setEndedAt(now);
                    workEntryRepository.save(open);
                });
        WorkEntry entry = new WorkEntry(now, description.trim(), user);
        return workEntryRepository.save(entry);
    }

    /**
     * Modifica il testo di una voce esistente, solo se appartiene all'utente.
     *
     * @throws AccessDeniedException se la voce non e' dell'utente o non esiste
     */
    public WorkEntry updateDescription(String username, Long entryId, String description) {
        User user = requireUser(username);
        WorkEntry entry = workEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Voce non trovata o non accessibile"));
        entry.setDescription(description.trim());
        return workEntryRepository.save(entry);
    }

    /**
     * Cancella una voce, solo se appartiene all'utente.
     *
     * @throws AccessDeniedException se la voce non e' dell'utente o non esiste
     */
    public void delete(String username, Long entryId) {
        User user = requireUser(username);
        WorkEntry entry = workEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Voce non trovata o non accessibile"));
        workEntryRepository.delete(entry);
    }

    /** Voci di oggi per l'utente, dalla piu' recente alla piu' vecchia. */
    public List<WorkEntry> todayEntries(String username, ZoneId zone) {
        User user = requireUser(username);
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
        return workEntryRepository.findByUserAndStartedAtBetweenOrderByStartedAtDesc(user, from, to);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }
}
