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
 * <p>Il tempo di una riga e' l'intervallo fino alla riga successiva in ordine cronologico
 * (quando si e' passati ad altro). La riga piu' recente in assoluto e' il task "in corso":
 * il suo tempo va da quando e' stata scritta fino ad <em>adesso</em>, cosi' il conteggio
 * resta corretto anche ripetendo lo stesso task. I tempi delle righe dello stesso contesto
 * vengono sommati e arrotondati <strong>per eccesso</strong> al quarto d'ora; il bottone
 * compare sulla riga piu' recente del contesto, con valore minimo 0,25.
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
        asc.sort(Comparator.comparing(WorkEntry::getCreatedAt));
        int n = asc.size();

        Map<String, Long> totalSecondsByContext = new HashMap<>();
        Map<String, WorkEntry> latestEntryByContext = new HashMap<>();

        for (int i = 0; i < n; i++) {
            WorkEntry entry = asc.get(i);
            Optional<String> context = taskLinkExtractor.firstTaskId(entry.getDescription());
            if (context.isEmpty()) {
                continue;
            }
            Instant end = (i < n - 1) ? asc.get(i + 1).getCreatedAt() : now;
            long seconds = Math.max(0, Duration.between(entry.getCreatedAt(), end).getSeconds());
            totalSecondsByContext.merge(context.get(), seconds, Long::sum);
            // asc e' in ordine crescente: l'ultimo assegnato e' la riga piu' recente del contesto
            latestEntryByContext.put(context.get(), entry);
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
