package com.consuntiver.service;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import com.consuntiver.repository.FixedTaskRepository;
import com.consuntiver.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class FixedTaskService {

    private final FixedTaskRepository fixedTaskRepository;
    private final UserRepository userRepository;

    public FixedTaskService(FixedTaskRepository fixedTaskRepository, UserRepository userRepository) {
        this.fixedTaskRepository = fixedTaskRepository;
        this.userRepository = userRepository;
    }

    /**
     * Crea un task fisso. Il numero task viene normalizzato a sole cifre (via '#' e spazi);
     * se non resta nulla viene salvato come assente (nessun link). Il nome, se vuoto, ricade
     * sul codice (o su "Task").
     */
    public FixedTask add(String username, String taskNumberRaw, String name, String description, int year) {
        User user = requireUser(username);
        String taskNumber = normalizeTaskNumber(taskNumberRaw);
        String finalName = (name != null && !name.isBlank())
                ? name.trim()
                : (taskNumber != null ? "#" + taskNumber : "Task");
        String finalDescription = (description != null && !description.isBlank()) ? description.trim() : null;
        return fixedTaskRepository.save(new FixedTask(taskNumber, finalName, finalDescription, year, user));
    }

    /** Cancella un task fisso, solo se appartiene all'utente. */
    public void delete(String username, Long id) {
        User user = requireUser(username);
        FixedTask task = fixedTaskRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Task non trovato o non accessibile"));
        fixedTaskRepository.delete(task);
    }

    /** Task fissi dell'utente raggruppati per anno (anni piu' recenti prima). */
    public List<YearGroup> listGroupedByYear(String username, String taskBaseUrl) {
        User user = requireUser(username);
        List<FixedTask> tasks = fixedTaskRepository.findByUserOrderByYearDescIdDesc(user);

        LinkedHashMap<Integer, List<FixedTaskView>> byYear = new LinkedHashMap<>();
        for (FixedTask t : tasks) {
            String url = t.getTaskNumber() != null ? EasyLinks.issueUrl(taskBaseUrl, t.getTaskNumber()) : null;
            byYear.computeIfAbsent(t.getYear(), y -> new ArrayList<>())
                    .add(new FixedTaskView(t.getId(), t.getTaskNumber(), url, t.getName(), t.getDescription()));
        }

        List<YearGroup> groups = new ArrayList<>();
        byYear.forEach((year, list) -> groups.add(new YearGroup(year, list)));
        return groups;
    }

    /** Estrae solo le cifre; se non ce ne sono restituisce null. */
    private String normalizeTaskNumber(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utente non trovato: " + username));
    }

    /**
     * Vista di un task fisso pronta per il template.
     *
     * @param url link a Easy, oppure null se il task non ha un numero
     */
    public record FixedTaskView(Long id, String taskNumber, String url, String name, String description) {
    }

    /** Gruppo di task fissi di uno stesso anno. */
    public record YearGroup(int year, List<FixedTaskView> tasks) {
    }
}
