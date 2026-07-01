package com.consuntiver.service;

import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.ContextTimeService.ContextButton;
import com.consuntiver.service.ContextTimeService.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextTimeServiceTest {

    private final ContextTimeService service = new ContextTimeService(new TaskLinkExtractor());

    private static final Instant BASE = Instant.parse("2026-06-30T07:00:00Z");

    private Instant at(int minutes) {
        return BASE.plus(minutes, ChronoUnit.MINUTES);
    }

    /** Riga con inizio e (opzionale) fine espressi in minuti da BASE. Fine null = riga in corso. */
    private WorkEntry entry(long id, int startMin, Integer endMin, String description) {
        WorkEntry e = new WorkEntry(at(startMin), description, null);
        e.setId(id);
        if (endMin != null) {
            e.setEndedAt(at(endMin));
        }
        return e;
    }

    @Test
    void sommaPerContestoEForniIlTotale() {
        List<WorkEntry> entries = List.of(
                entry(1, 0, 30, "Lavoro su #12345"),      // 30 min, task 12345
                entry(2, 30, 60, "Rispondo a una mail"),   // 30 min, contesto testo
                entry(3, 60, 120, "Ancora su #12345"),     // 60 min, task 12345
                entry(4, 120, null, "Riunione #99999")     // in corso: 120 -> now(150) = 30 min
        );

        Map<Long, ContextButton> b = service.compute(entries, at(150)).buttons();

        // task 12345: 30 + 60 = 90 min, mostrato sulla riga piu' recente (id 3)
        assertThat(b.get(3L).label()).isEqualTo("+1,5");
        assertThat(b.get(3L).actual()).isEqualTo("1:30");
        // la mail (contesto testo) ha il suo tempo sulla propria riga
        assertThat(b.get(2L).actual()).isEqualTo("0:30");
        // task 99999 in corso: 30 min fino ad adesso
        assertThat(b.get(4L).actual()).isEqualTo("0:30");
        // id 1 e' 12345 ma non e' la riga piu' recente del contesto -> niente bottone
        assertThat(b).doesNotContainKey(1L);

        Result r = service.compute(entries, at(150));
        // Totale: 1,5 + 0,5 + 0,5 = 2,5 h ; effettivo 90 + 30 + 30 = 150 min
        assertThat(r.totalLabel()).isEqualTo("2,5");
        assertThat(r.totalActual()).isEqualTo("2:30");
    }

    @Test
    void arrotondaSemprePerEccessoAlQuartoDOra() {
        assertThat(quartersFor(1)).isEqualTo("+0,25");
        assertThat(quartersFor(15)).isEqualTo("+0,25");
        assertThat(quartersFor(16)).isEqualTo("+0,5");
        assertThat(quartersFor(22)).isEqualTo("+0,5");
        assertThat(quartersFor(46)).isEqualTo("+1");
    }

    /** Un task con durata chiusa di 'minutes' minuti; ritorna l'etichetta del suo bottone. */
    private String quartersFor(int minutes) {
        Result r = service.compute(List.of(
                entry(100, 0, minutes, "Task #11111")), at(minutes + 5));
        return r.buttons().get(100L).label();
    }

    @Test
    void ripetereLoStessoTaskInCorsoConteggiaFinoAdAdesso() {
        List<WorkEntry> entries = List.of(
                entry(10, 0, 30, "#33333 inizio"),
                entry(11, 30, null, "#33333 continuo"));   // aperta: 30 -> now(50) = 20 min
        Map<Long, ContextButton> b = service.compute(entries, at(50)).buttons();
        // 30 + 20 = 50 min sul task 33333, sulla riga piu' recente
        assertThat(b.get(11L).actual()).isEqualTo("0:50");
        assertThat(b.get(11L).label()).isEqualTo("+1"); // 50 min -> per eccesso a 1 h
    }

    @Test
    void raggruppaAnchePerStessoTesto() {
        List<WorkEntry> entries = List.of(
                entry(50, 0, 30, "Pausa caffe"),
                entry(51, 30, null, "Pausa caffe"));       // stesso testo, in corso
        Result r = service.compute(entries, at(45));       // 30 + 15 = 45 min
        assertThat(r.buttons().get(51L).actual()).isEqualTo("0:45");
        assertThat(r.buttons()).doesNotContainKey(50L);
        assertThat(r.hasTotal()).isTrue();
    }

    @Test
    void laModificaDelTaskRiadeguaIlConteggioSenzaDuplicare() {
        // Prima: la riga di mezzo ha il task SBAGLIATO
        List<WorkEntry> prima = List.of(
                entry(40, 0, 30, "#44444 analisi"),
                entry(41, 30, 60, "#44445 sviluppo"),      // errore di battitura
                entry(42, 60, null, "#44444 chiusura"));
        Map<Long, ContextButton> b1 = service.compute(prima, at(90)).buttons();
        assertThat(b1.get(42L).actual()).isEqualTo("1:00"); // 30 + 30 su 44444
        assertThat(b1.get(41L).actual()).isEqualTo("0:30"); // 30 su 44445

        // Dopo la correzione della riga 41 a #44444: stessa lista, testo corretto
        List<WorkEntry> dopo = List.of(
                entry(40, 0, 30, "#44444 analisi"),
                entry(41, 30, 60, "#44444 sviluppo"),      // corretto
                entry(42, 60, null, "#44444 chiusura"));
        Result r2 = service.compute(dopo, at(90));
        assertThat(r2.buttons().get(42L).actual()).isEqualTo("1:30"); // tutto su 44444
        assertThat(r2.buttons()).doesNotContainKey(41L);              // niente residuo su 44445
        assertThat(r2.totalActual()).isEqualTo("1:30");
        assertThat(r2.totalLabel()).isEqualTo("1,5");
    }
}
