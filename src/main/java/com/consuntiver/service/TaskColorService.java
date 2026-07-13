package com.consuntiver.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Assegna a ogni task un colore (tinta HSL) e genera il CSS con le classi
 * {@code .tc-<id>} che impostano la variabile {@code --tc}.
 *
 * <p>La tinta di base deriva (in modo stabile) dal codice del task, cosi' non cambia
 * a ogni ricarica; ma all'interno della stessa pagina i colori vengono resi il piu'
 * possibile distinti: si sceglie, per ogni task, la tinta piu' lontana da quelle gia'
 * assegnate, evitando che due task finiscano con colori troppo simili.
 */
@Service
public class TaskColorService {

    /** Distanza (in gradi di tinta) considerata "sufficiente" tra due task. */
    private static final int MIN_HUE_SEPARATION = 40;
    /** Passo con cui si esplora la ruota dei colori: coprimo con 360, buona dispersione. */
    private static final int STEP = 47;

    /**
     * CSS con una regola {@code .tc-<id>} per ogni id (nell'ordine dato, che indica la
     * priorita': i primi mantengono piu' facilmente la tinta di base). Gli id duplicati
     * vengono ignorati.
     */
    public String buildCss(List<String> taskIds) {
        List<Integer> usedHues = new ArrayList<>();
        StringBuilder css = new StringBuilder();
        for (String id : new LinkedHashSet<>(taskIds)) {
            int hue = pickHue(id, usedHues);
            usedHues.add(hue);
            css.append(".tc-").append(id)
                    .append("{--tc:hsl(").append(hue).append(", 65%, 48%);}");
        }
        return css.toString();
    }

    /** Tinta per un singolo task, la piu' distante possibile da quelle gia' usate. */
    private int pickHue(String taskId, List<Integer> used) {
        int base = Math.floorMod(taskId.hashCode(), 360);
        if (used.isEmpty()) {
            return base;
        }
        int bestHue = base;
        int bestDistance = -1;
        // Parte dalla tinta di base e gira la ruota a passi coprimi con 360: cosi' le
        // visita tutte, restando vicino alla base finche' possibile (colori stabili).
        for (int i = 0; i < 360; i++) {
            int hue = Math.floorMod(base + i * STEP, 360);
            int distance = minDistance(hue, used);
            if (distance >= MIN_HUE_SEPARATION) {
                return hue; // abbastanza distinto: va bene
            }
            if (distance > bestDistance) {
                bestDistance = distance;
                bestHue = hue;
            }
        }
        // Troppi task per separarli tutti: prende comunque la tinta piu' distante.
        return bestHue;
    }

    /** Minima distanza angolare (0..180) tra una tinta e un insieme di tinte. */
    private int minDistance(int hue, List<Integer> used) {
        int min = 180;
        for (int u : used) {
            int d = Math.abs(hue - u);
            min = Math.min(min, Math.min(d, 360 - d));
        }
        return min;
    }
}
