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
import java.util.Optional;

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

        // Se esiste gia' un task con lo stesso (utente, codice, anno) - anche cestinato -
        // lo riusiamo invece di crearne un duplicato: cosi' non si viola il vincolo di
        // unicita' (id_user, cod_task, year) e riaggiungere un numero cestinato lo ripristina.
        if (taskNumber != null) {
            Optional<FixedTask> existing =
                    fixedTaskRepository.findFirstByUserAndTaskNumberAndYear(user, taskNumber, year);
            if (existing.isPresent()) {
                FixedTask task = existing.get();
                task.setDeleted(false);
                task.setName(finalName);
                task.setDescription(finalDescription);
                return fixedTaskRepository.save(task);
            }
        }
        return fixedTaskRepository.save(new FixedTask(taskNumber, finalName, finalDescription, year, user));
    }

    /** Task ATTIVI dell'utente che hanno un codice, per il selettore sulla barra attivita' (dedup per codice). */
    public List<TaskOption> options(String username) {
        User user = requireUser(username);
        LinkedHashMap<String, TaskOption> byCode = new LinkedHashMap<>();
        for (FixedTask t : fixedTaskRepository.findByUserAndDeletedFalseOrderByYearDescIdDesc(user)) {
            if (t.getTaskNumber() != null) {
                byCode.putIfAbsent(t.getTaskNumber(), new TaskOption(t.getTaskNumber(), t.getName()));
            }
        }
        return new ArrayList<>(byCode.values());
    }

    /** Modifica un task fisso, solo se appartiene all'utente. */
    public FixedTask update(String username, Long id, String taskNumberRaw,
                            String name, String description, int year) {
        User user = requireUser(username);
        FixedTask task = fixedTaskRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Task non trovato o non accessibile"));
        String taskNumber = normalizeTaskNumber(taskNumberRaw);
        task.setTaskNumber(taskNumber);
        task.setName((name != null && !name.isBlank())
                ? name.trim()
                : (taskNumber != null ? "#" + taskNumber : "Task"));
        task.setDescription((description != null && !description.isBlank()) ? description.trim() : null);
        task.setYear(year);
        return fixedTaskRepository.save(task);
    }

    /**
     * Elimina (logicamente) un task fisso, solo se appartiene all'utente: imposta il flag
     * {@code deleted}. Il task sparisce dagli attivi e dal selettore ma resta a DB (cosi'
     * non si rompe il collegamento con le attivita' che lo citano) ed e' ripristinabile.
     */
    public void delete(String username, Long id) {
        setDeleted(username, id, true);
    }

    /** Ripristina un task precedentemente eliminato (deleted = false). */
    public void restore(String username, Long id) {
        setDeleted(username, id, false);
    }

    private void setDeleted(String username, Long id, boolean deleted) {
        User user = requireUser(username);
        FixedTask task = fixedTaskRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Task non trovato o non accessibile"));
        task.setDeleted(deleted);
        fixedTaskRepository.save(task);
    }

    /** Task fissi ATTIVI dell'utente raggruppati per anno (anni piu' recenti prima). */
    public List<YearGroup> listGroupedByYear(String username, String taskBaseUrl) {
        User user = requireUser(username);
        List<FixedTask> tasks = fixedTaskRepository.findByUserAndDeletedFalseOrderByYearDescIdDesc(user);

        LinkedHashMap<Integer, List<FixedTaskView>> byYear = new LinkedHashMap<>();
        for (FixedTask t : tasks) {
            byYear.computeIfAbsent(t.getYear(), y -> new ArrayList<>()).add(toView(t, taskBaseUrl));
        }

        List<YearGroup> groups = new ArrayList<>();
        byYear.forEach((year, list) -> groups.add(new YearGroup(year, list)));
        return groups;
    }

    /** Task ELIMINATI (logicamente) dell'utente, in elenco piatto, i piu' recenti prima. */
    public List<FixedTaskView> listDeleted(String username, String taskBaseUrl) {
        User user = requireUser(username);
        List<FixedTaskView> deleted = new ArrayList<>();
        for (FixedTask t : fixedTaskRepository.findByUserAndDeletedTrueOrderByYearDescIdDesc(user)) {
            deleted.add(toView(t, taskBaseUrl));
        }
        return deleted;
    }

    private FixedTaskView toView(FixedTask t, String taskBaseUrl) {
        String url = t.getTaskNumber() != null ? EasyLinks.issueUrl(taskBaseUrl, t.getTaskNumber()) : null;
        return new FixedTaskView(t.getId(), t.getTaskNumber(), url, t.getName(), t.getDescription());
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

    /** Opzione del selettore task (codice + nome). */
    public record TaskOption(String code, String name) {
    }
}
