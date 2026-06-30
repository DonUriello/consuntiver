package com.consuntiver.service;

import com.consuntiver.model.WorkEntry;
import com.consuntiver.service.ContextTimeService.ContextButton;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextTimeServiceTest {

    private final ContextTimeService service = new ContextTimeService(new TaskLinkExtractor());

    private static final Instant BASE = Instant.parse("2026-06-30T07:00:00Z");

    private WorkEntry entry(long id, int minutesFromBase, String description) {
        WorkEntry e = new WorkEntry(BASE.plus(minutesFromBase, ChronoUnit.MINUTES), description, null);
        e.setId(id);
        return e;
    }

    @Test
    void sommaIlTempoDelloStessoTaskSullaRigaPiuRecente() {
        List<WorkEntry> entries = List.of(
                entry(1, 0, "Lavoro su #12345"),     // 30 min su 12345
                entry(2, 30, "Rispondo a una mail"),  // 30 min, nessun task
                entry(3, 60, "Ancora su #12345"),     // 60 min su 12345
                entry(4, 120, "Riunione #99999")      // ultima riga: 0 min
        );

        Map<Long, ContextButton> buttons = service.compute(entries);

        // #12345: 30 + 60 = 90 min = 1,5 h, mostrato sulla riga piu' recente (id 3)
        assertThat(buttons).containsKey(3L);
        assertThat(buttons.get(3L).label()).isEqualTo("+1,5");
        // #99999: ultima riga senza successiva -> 0 -> nessun bottone
        assertThat(buttons).doesNotContainKey(4L);
        // le righe senza task non hanno bottone
        assertThat(buttons).doesNotContainKey(1L);
        assertThat(buttons).doesNotContainKey(2L);
    }

    @Test
    void arrotondaAlQuartoDOraEFormattaConIlPiu() {
        // 1 task con 22 minuti -> arrotonda a 0,25 h
        Map<Long, ContextButton> q = service.compute(List.of(
                entry(10, 0, "Task #11111"),
                entry(11, 22, "Stop")));
        assertThat(q.get(10L).label()).isEqualTo("+0,25");

        // 2 ore esatte -> "+2" (senza decimali)
        Map<Long, ContextButton> due = service.compute(List.of(
                entry(20, 0, "Task #22222"),
                entry(21, 120, "Stop")));
        assertThat(due.get(20L).label()).isEqualTo("+2");

        // 7 minuti -> arrotonda a 0 -> sotto soglia, nessun bottone
        Map<Long, ContextButton> piccolo = service.compute(List.of(
                entry(30, 0, "Task #33333"),
                entry(31, 7, "Stop")));
        assertThat(piccolo).isEmpty();
    }

    @Test
    void linkPlaceholderPresente() {
        Map<Long, ContextButton> buttons = service.compute(List.of(
                entry(40, 0, "Task #44444"),
                entry(41, 45, "Stop")));
        assertThat(buttons.get(40L).label()).isEqualTo("+0,75");
        assertThat(buttons.get(40L).url()).isEqualTo("#");
    }
}
