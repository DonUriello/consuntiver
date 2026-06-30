package com.consuntiver.service;

import com.consuntiver.model.WorkEntry;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Riconosce i task citati nelle righe dello storico e ne costruisce i link verso Easy.
 *
 * <p>Un task e' un numero scritto come {@code #12345} oppure come numero "nudo".
 * Per evitare falsi positivi (es. "8 ore", l'anno "2026", l'orario "0930"), il numero
 * nudo e' considerato un task solo se ha almeno 5 cifre; con il prefisso {@code #}
 * basta una cifra.
 */
@Component
public class TaskLinkExtractor {

    /** {@code #<cifre>} oppure un numero nudo di almeno 5 cifre non incollato ad altri caratteri. */
    private static final Pattern TASK_PATTERN =
            Pattern.compile("#(\\d+)|(?<![\\w.])(\\d{5,})(?![\\w.])");

    /**
     * Estrae i task dalle righe fornite. Un task citato in piu' righe compare una sola
     * volta (mantenendo la descrizione della prima riga in cui appare).
     */
    public List<TaskLink> extract(List<WorkEntry> entries) {
        LinkedHashMap<String, TaskLink> byId = new LinkedHashMap<>();
        for (WorkEntry entry : entries) {
            String text = entry.getDescription();
            Matcher matcher = TASK_PATTERN.matcher(text);
            while (matcher.find()) {
                String id = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                byId.putIfAbsent(id, new TaskLink(id, EasyLinks.issueUrl(id), text.trim()));
            }
        }
        return new ArrayList<>(byId.values());
    }

    /**
     * Un task rilevato.
     *
     * @param id          numero del task, senza {@code #}
     * @param url         link alla issue su Easy
     * @param description testo della riga in cui il task e' stato trovato
     */
    public record TaskLink(String id, String url, String description) {
    }
}
