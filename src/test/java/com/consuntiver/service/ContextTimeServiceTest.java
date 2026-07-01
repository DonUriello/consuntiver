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

    private WorkEntry entry(long id, int minutesFromBase, String description) {
        WorkEntry e = new WorkEntry(at(minutesFromBase), description, null);
        e.setId(id);
        return e;
    }

    @Test
    void sommaIlTempoDelloStessoTaskEForniIlTotale() {
        List<WorkEntry> entries = List.of(
                entry(1, 0, "Lavoro su #12345"),     // 30 min su 12345
                entry(2, 30, "Rispondo a una mail"),  // 30 min, nessun task
                entry(3, 60, "Ancora su #12345"),     // 60 min su 12345
                entry(4, 120, "Riunione #99999")      // in corso: 120 -> now(150) = 30 min
        );

        Result r = service.compute(entries, at(150));
        Map<Long, ContextButton> b = r.buttons();

        // #12345: 30 + 60 = 90 min = 1,5 h, sulla riga piu' recente (id 3)
        assertThat(b.get(3L).label()).isEqualTo("+1,5");
        assertThat(b.get(3L).actual()).isEqualTo("1:30");
        // #99999: riga in corso, 30 min fino ad adesso
        assertThat(b.get(4L).label()).isEqualTo("+0,5");
        assertThat(b.get(4L).actual()).isEqualTo("0:30");
        // righe senza task o non piu' recenti del contesto: nessun bottone
        assertThat(b).doesNotContainKeys(1L, 2L);

        // Totale: 1,5 + 0,5 = 2 h ; effettivo 90 + 30 = 120 min
        assertThat(r.totalLabel()).isEqualTo("2");
        assertThat(r.totalActual()).isEqualTo("2:00");
    }

    @Test
    void arrotondaSemprePerEccessoAlQuartoDOra() {
        // 1 minuto -> per eccesso a 0,25
        assertThat(quartersFor(1)).isEqualTo("+0,25");
        // 15 minuti esatti -> resta 0,25
        assertThat(quartersFor(15)).isEqualTo("+0,25");
        // 16 minuti -> supera il quarto -> 0,5
        assertThat(quartersFor(16)).isEqualTo("+0,5");
        // 22 minuti -> 0,5
        assertThat(quartersFor(22)).isEqualTo("+0,5");
        // 46 minuti -> 0,75 arrotondato per eccesso a 1
        assertThat(quartersFor(46)).isEqualTo("+1");
    }

    /** Tempo su un task che NON e' l'ultima riga (segmento chiuso, indipendente da now). */
    private String quartersFor(int minutes) {
        Result r = service.compute(List.of(
                entry(100, 0, "Task #11111"),
                entry(101, minutes, "Altro senza task")), at(minutes + 5));
        return r.buttons().get(100L).label();
    }

    @Test
    void ripetereLoStessoTaskInCorsoConteggiaFinoAdAdesso() {
        // Stesso task scritto due volte, ed e' il task in corso
        List<WorkEntry> entries = List.of(
                entry(10, 0, "#33333 inizio"),
                entry(11, 30, "#33333 continuo"));
        // 0->30 = 30 min, poi 30->now(50) = 20 min -> totale 50 min
        Result r = service.compute(entries, at(50));
        assertThat(r.buttons().get(11L).actual()).isEqualTo("0:50");
        assertThat(r.buttons().get(11L).label()).isEqualTo("+1"); // 50 min -> per eccesso a 1 h
    }

    @Test
    void laModificaDelTaskRiadeguaIlConteggioSenzaDuplicare() {
        // Prima: la riga di mezzo ha il task SBAGLIATO (#44445 invece di #44444)
        List<WorkEntry> prima = List.of(
                entry(40, 0, "#44444 analisi"),
                entry(41, 30, "#44445 sviluppo"),   // errore di battitura
                entry(42, 60, "#44444 chiusura"));
        Result r1 = service.compute(prima, at(90));
        // #44444 = 30 (id40) + 30 (id42->now90) = 60 min ; #44445 = 30 min (riga id41)
        assertThat(r1.buttons().get(42L).actual()).isEqualTo("1:00");
        assertThat(r1.buttons().get(41L).actual()).isEqualTo("0:30");

        // Dopo la correzione della riga 41 a #44444: ricalcolo sulla lista aggiornata
        List<WorkEntry> dopo = List.of(
                entry(40, 0, "#44444 analisi"),
                entry(41, 30, "#44444 sviluppo"),   // corretto
                entry(42, 60, "#44444 chiusura"));
        Result r2 = service.compute(dopo, at(90));
        // Ora tutto e' #44444 = 90 min, e #44445 non esiste piu' (nessun conteggio residuo)
        assertThat(r2.buttons().get(42L).actual()).isEqualTo("1:30");
        assertThat(r2.buttons()).doesNotContainKey(41L);
        assertThat(r2.totalActual()).isEqualTo("1:30");
        assertThat(r2.totalLabel()).isEqualTo("1,5");
    }

    @Test
    void nessunTaskNessunTotale() {
        Result r = service.compute(List.of(
                entry(50, 0, "solo testo"),
                entry(51, 30, "altro testo")), at(60));
        assertThat(r.buttons()).isEmpty();
        assertThat(r.hasTotal()).isFalse();
    }
}
