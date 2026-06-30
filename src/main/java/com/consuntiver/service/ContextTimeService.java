package com.consuntiver.service;

import com.consuntiver.model.WorkEntry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcola, per ogni "contesto" (task) citato nelle righe del giorno, il tempo totale
 * dedicato, e individua su quale riga mostrare il bottone con il totale.
 *
 * <p>Il tempo di una riga e' l'intervallo fino alla riga successiva in ordine cronologico
 * (quando si e' passati ad altro). La riga piu' recente in assoluto non ha una successiva,
 * quindi non contribuisce. I tempi delle righe dello stesso contesto vengono sommati e
 * arrotondati al quarto d'ora; il bottone compare sulla riga piu' recente del contesto.
 * Anche quando il totale non raggiunge un quarto d'ora il bottone viene mostrato comunque,
 * con il valore minimo di 0,25.
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
     * @return mappa id-riga -> bottone, solo per le righe che devono mostrarlo
     */
    public Map<Long, ContextButton> compute(List<WorkEntry> entries) {
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
            long seconds = (i < n - 1)
                    ? Duration.between(entry.getCreatedAt(), asc.get(i + 1).getCreatedAt()).getSeconds()
                    : 0;
            totalSecondsByContext.merge(context.get(), seconds, Long::sum);
            // asc e' in ordine crescente: l'ultimo assegnato e' la riga piu' recente del contesto
            latestEntryByContext.put(context.get(), entry);
        }

        Map<Long, ContextButton> buttons = new HashMap<>();
        latestEntryByContext.forEach((context, entry) -> {
            long seconds = totalSecondsByContext.getOrDefault(context, 0L);
            // Anche sotto il quarto d'ora si mostra comunque il bottone, con il minimo 0,25.
            double quarters = Math.max(MIN_QUARTERS, roundToQuarter(seconds));
            buttons.put(entry.getId(), new ContextButton(format(quarters), formatActual(seconds), LINK_PLACEHOLDER));
        });
        return buttons;
    }

    /** Ore arrotondate al quarto d'ora piu' vicino. */
    private double roundToQuarter(long seconds) {
        double hours = seconds / 3600.0;
        return Math.round(hours * 4) / 4.0;
    }

    /** Formatta il numero con il '+' davanti, in stile italiano (es. {@code +0,75}, {@code +2}). */
    private String format(double hours) {
        String number = BigDecimal.valueOf(hours).stripTrailingZeros().toPlainString();
        return "+" + number.replace('.', ',');
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
     * @param label  etichetta arrotondata al quarto d'ora (es. {@code +0,75})
     * @param actual tempo effettivo non arrotondato (es. {@code 0:47})
     * @param url    link, al momento non definito
     */
    public record ContextButton(String label, String actual, String url) {
    }
}
