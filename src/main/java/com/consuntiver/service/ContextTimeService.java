package com.consuntiver.service;

import com.consuntiver.model.WorkEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcola, per ogni "contesto" (task) citato nelle righe del giorno, il tempo totale
 * dedicato, individua su quale riga mostrare il bottone col totale e fornisce il totale
 * complessivo della giornata.
 *
 * <p>Il tempo di una riga e' la differenza tra la sua fine e il suo inizio (salvati sulla
 * riga stessa). La riga ancora aperta (senza fine) e' quella "in corso": conta fino ad
 * <em>adesso</em>. Il contesto di una riga e' il suo task, oppure — se non ne cita nessuno —
 * il testo inserito: righe con lo stesso task o lo stesso testo vengono raggruppate e i loro
 * tempi sommati, arrotondati <strong>per eccesso</strong> al quarto d'ora (minimo 0,25).
 * Il totale del contesto compare solo sulla riga piu' recente del contesto; le righe piu'
 * vecchie dello stesso contesto non mostrano il tempo.
 *
 * <p>Essendo una funzione pura delle righe, viene rieseguita a ogni caricamento della pagina
 * (quindi anche dopo ogni modifica di una riga): il conteggio si ri-adegua automaticamente.
 */
@Service
public class ContextTimeService {

    /** Link del bottone: non ancora definito. */
    private static final String LINK_PLACEHOLDER = "#";

    /** Valore minimo mostrato sul bottone: un quarto d'ora. */
    private static final double MIN_QUARTERS = 0.25;

    private final TaskLinkExtractor taskLinkExtractor;

    public ContextTimeService(TaskLinkExtractor taskLinkExtractor) {
        this.taskLinkExtractor = taskLinkExtractor;
    }

    /**
     * @param entries righe del giorno (in qualsiasi ordine)
     * @param now     istante corrente, usato per il tempo del task in corso (riga piu' recente)
     */
    public Result compute(List<WorkEntry> entries, Instant now) {
        List<WorkEntry> asc = new ArrayList<>(entries);
        asc.sort(Comparator.comparing(WorkEntry::getStartedAt));

        Map<String, Long> totalSecondsByContext = new HashMap<>();
        Map<String, WorkEntry> latestEntryByContext = new HashMap<>();

        for (WorkEntry entry : asc) {
            String context = contextKey(entry.getDescription());
            // Durata della riga: fine - inizio; se la riga e' aperta, conta fino ad adesso.
            Instant end = entry.getEndedAt() != null ? entry.getEndedAt() : now;
            long seconds = Math.max(0, Duration.between(entry.getStartedAt(), end).getSeconds());
            totalSecondsByContext.merge(context, seconds, Long::sum);
            // asc e' in ordine crescente: l'ultimo assegnato e' la riga piu' recente del contesto
            latestEntryByContext.put(context, entry);
        }

        Map<Long, ContextButton> buttons = new HashMap<>();
        double totalQuarters = 0;
        long totalSeconds = 0;
        for (Map.Entry<String, WorkEntry> e : latestEntryByContext.entrySet()) {
            long seconds = totalSecondsByContext.getOrDefault(e.getKey(), 0L);
            // Arrotondamento per eccesso; anche sotto il quarto d'ora si mostra il minimo 0,25.
            double quarters = Math.max(MIN_QUARTERS, ceilToQuarter(seconds));
            buttons.put(e.getValue().getId(),
                    new ContextButton(withPlus(quarters), formatActual(seconds), LINK_PLACEHOLDER));
            totalQuarters += quarters;
            totalSeconds += seconds;
        }

        return new Result(buttons, !buttons.isEmpty(),
                formatNumber(totalQuarters), formatActual(totalSeconds));
    }

    /**
     * Chiave del contesto di una riga: il task citato, altrimenti il testo normalizzato.
     * Cosi' righe con lo stesso task, o con lo stesso testo, finiscono nello stesso gruppo.
     */
    private String contextKey(String description) {
        Optional<String> task = taskLinkExtractor.firstTaskId(description);
        if (task.isPresent()) {
            return "task:" + task.get();
        }
        String normalized = description == null ? "" : description.trim().toLowerCase().replaceAll("\\s+", " ");
        return "text:" + normalized;
    }

    /** Ore arrotondate <strong>per eccesso</strong> al quarto d'ora. */
    private double ceilToQuarter(long seconds) {
        double hours = seconds / 3600.0;
        return Math.ceil(hours * 4) / 4.0;
    }

    /** Numero in stile italiano senza decimali inutili (es. {@code 0,75}, {@code 2}). */
    private String formatNumber(double hours) {
        return BigDecimal.valueOf(hours).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Come {@link #formatNumber} ma con il {@code +} davanti (es. {@code +0,75}). */
    private String withPlus(double hours) {
        return "+" + formatNumber(hours);
    }

    /** Tempo effettivo (non arrotondato) come {@code h:mm}, es. {@code 0:47}, {@code 1:30}. */
    private String formatActual(long seconds) {
        long totalMinutes = Math.round(seconds / 60.0);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours + ":" + (minutes < 10 ? "0" + minutes : Long.toString(minutes));
    }

    /**
     * Bottone del tempo di contesto.
     *
     * @param label  etichetta arrotondata per eccesso al quarto d'ora (es. {@code +0,75})
     * @param actual tempo effettivo non arrotondato (es. {@code 0:47})
     * @param url    link, al momento non definito
     */
    public record ContextButton(String label, String actual, String url) {
    }

    /**
     * Risultato del calcolo: bottoni per riga e totale complessivo della giornata.
     *
     * @param buttons     id-riga -> bottone, solo per le righe che devono mostrarlo
     * @param hasTotal    true se c'e' almeno un contesto (quindi un totale da mostrare)
     * @param totalLabel  totale in quarti d'ora sommati (es. {@code 2,75})
     * @param totalActual totale effettivo come {@code h:mm} (es. {@code 2:32})
     */
    public record Result(Map<Long, ContextButton> buttons, boolean hasTotal,
                         String totalLabel, String totalActual) {
    }
}
