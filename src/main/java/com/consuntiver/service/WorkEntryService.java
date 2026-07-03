package com.consuntiver.service;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import com.consuntiver.model.WorkEntry;
import com.consuntiver.repository.FixedTaskRepository;
import com.consuntiver.repository.UserRepository;
import com.consuntiver.repository.WorkEntryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class WorkEntryService {

    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

    private final WorkEntryRepository workEntryRepository;
    private final UserRepository userRepository;
    private final FixedTaskRepository fixedTaskRepository;
    private final TaskLinkExtractor taskLinkExtractor;

    public WorkEntryService(WorkEntryRepository workEntryRepository,
                            UserRepository userRepository,
                            FixedTaskRepository fixedTaskRepository,
                            TaskLinkExtractor taskLinkExtractor) {
        this.workEntryRepository = workEntryRepository;
        this.userRepository = userRepository;
        this.fixedTaskRepository = fixedTaskRepository;
        this.taskLinkExtractor = taskLinkExtractor;
    }

    /**
     * Salva una nuova voce con l'ora corrente come inizio. All'inserimento della nuova riga
     * la voce precedente ancora aperta viene chiusa: la sua fine diventa "adesso".
     * Se il testo cita un #codice, il task viene trovato o creato e collegato.
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
        entry.setTask(resolveTask(user, entry.getDescription()));
        return workEntryRepository.save(entry);
    }

    /**
     * Modifica il testo di una voce esistente, solo se appartiene all'utente.
     * Ri-risolve il task collegato in base al nuovo testo.
     *
     * @throws AccessDeniedException se la voce non e' dell'utente o non esiste
     */
    public WorkEntry updateDescription(String username, Long entryId, String description) {
        User user = requireUser(username);
        WorkEntry entry = workEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Voce non trovata o non accessibile"));
        entry.setDescription(description.trim());
        entry.setTask(resolveTask(user, entry.getDescription()));
        return workEntryRepository.save(entry);
    }

    /**
     * Ricava il task dal testo: se cita un #codice, cerca il task dell'utente per codice/anno
     * corrente e, se non esiste, lo crea. Restituisce null se il testo non cita alcun task.
     */
    private FixedTask resolveTask(User user, String description) {
        Optional<String> code = taskLinkExtractor.firstTaskId(description);
        if (code.isEmpty()) {
            return null;
        }
        int year = LocalDate.now(ZONE).getYear();
        return fixedTaskRepository.findFirstByUserAndTaskNumberAndYear(user, code.get(), year)
                .orElseGet(() -> fixedTaskRepository.save(
                        new FixedTask(code.get(), "#" + code.get(), null, year, user)));
    }

    /**
     * Termina un'attivita' ancora aperta: ne imposta la fine ad "adesso".
     * Non fa nulla se la voce ha gia' una fine.
     *
     * @throws AccessDeniedException se la voce non e' dell'utente o non esiste
     */
    public void endActivity(String username, Long entryId) {
        User user = requireUser(username);
        WorkEntry entry = workEntryRepository.findById(entryId)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Voce non trovata o non accessibile"));
        if (entry.getEndedAt() == null) {
            entry.setEndedAt(Instant.now());
            workEntryRepository.save(entry);
        }
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
