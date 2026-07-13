(function () {
    "use strict";

    // ---------- Contatore live del tempo di lavoro ----------
    // Il timer conta il lavoro NETTO (esclusa la pausa pranzo) verso le 8 ore, a
    // partire dall'orario di entrata inserito a mano. La pausa e' quella effettiva
    // se sono presenti sia inizio che rientro, altrimenti si assume 1 ora.
    var el = document.getElementById("worktime");
    if (el) {
        var num = function (key) {
            var v = el.dataset[key];
            return (v === undefined || v === "" || v === "null") ? null : parseInt(v, 10);
        };
        var entry = num("entry");
        var lunchStart = num("lunchStart");
        var lunchEnd = num("lunchEnd");
        var exit = num("exit");
        var target = parseInt(el.dataset.target || "28800", 10);
        var defaultBreak = parseInt(el.dataset.defaultBreak || "3600", 10) * 1000;
        // Pausa pranzo minima: una pausa piu' breve di questo vale comunque questo.
        var minBreak = parseInt(el.dataset.minBreak || "2700", 10) * 1000;
        var serverNow = parseInt(el.dataset.serverNow || Date.now(), 10);
        // Differenza tra orologio del server e del browser, per non sballare il conteggio.
        var offset = serverNow - Date.now();

        var counterEl = document.getElementById("wt-counter");
        var barEl = document.getElementById("wt-bar");
        var statusEl = document.getElementById("wt-status");

        var pad = function (n) { return n < 10 ? "0" + n : "" + n; };

        var fmt = function (totalSeconds) {
            var s = Math.max(0, Math.floor(totalSeconds));
            return pad(Math.floor(s / 3600)) + ":" + pad(Math.floor((s % 3600) / 60)) + ":" + pad(s % 60);
        };
        var hm = function (totalSeconds) {
            var s = Math.max(0, Math.floor(totalSeconds));
            return pad(Math.floor(s / 3600)) + ":" + pad(Math.floor((s % 3600) / 60));
        };
        // Orario nel fuso Europe/Rome, coerente con quanto salvato lato server.
        var clockLabel = function (ms) {
            return new Date(ms).toLocaleTimeString("it-IT",
                    { timeZone: "Europe/Rome", hour: "2-digit", minute: "2-digit" });
        };

        // Secondi di lavoro netto fino a "end" (ms).
        var workedSeconds = function (end) {
            if (end < entry) { end = entry; }
            var ms;
            if (lunchStart !== null && lunchEnd !== null) {
                if (end <= lunchStart) { ms = end - entry; }
                else if (end < lunchEnd) { ms = lunchStart - entry; }
                // Pausa completata: sottrai almeno il minimo (45 min) anche se piu' breve.
                else { ms = (end - entry) - Math.max(lunchEnd - lunchStart, minBreak); }
            } else if (lunchStart !== null) {
                ms = (end <= lunchStart) ? (end - entry) : (lunchStart - entry);
            } else {
                ms = end - entry;
            }
            return ms / 1000;
        };

        var tick = function () {
            var now = Date.now() + offset;
            var end = (exit !== null) ? exit : now;
            var onBreak = exit === null && lunchStart !== null && end > lunchStart
                    && (lunchEnd === null || end < lunchEnd);

            var worked = workedSeconds(end);
            counterEl.textContent = fmt(worked);
            barEl.style.width = Math.min(100, (worked / target) * 100).toFixed(1) + "%";
            barEl.classList.toggle("done", worked >= target);

            // Pausa considerata per stimare l'uscita: effettiva (minimo 45 min) se
            // completa, altrimenti 1h di default.
            var breakMs = (lunchStart !== null && lunchEnd !== null)
                    ? Math.max(lunchEnd - lunchStart, minBreak) : defaultBreak;
            if (onBreak && lunchEnd === null) {
                breakMs = Math.max(defaultBreak, now - lunchStart);
            }
            var expectedExit = entry + target * 1000 + breakMs;

            if (exit !== null) {
                statusEl.textContent = worked >= target
                        ? "✅ Giornata completata: " + hm(worked) + " lavorate."
                        : "Uscita registrata: " + hm(worked) + " lavorate (sotto le 8 ore).";
            } else if (onBreak) {
                statusEl.textContent = "⏸ In pausa pranzo — " + hm(worked) + " lavorate finora.";
            } else if (worked >= target) {
                statusEl.textContent = "✅ Hai completato le 8 ore! Puoi uscire.";
            } else {
                statusEl.textContent = "Mancano " + hm(target - worked)
                        + " alle 8 ore · uscita prevista ~" + clockLabel(expectedExit);
            }
        };

        if (entry === null) {
            counterEl.textContent = "00:00:00";
            barEl.style.width = "0%";
            statusEl.textContent = "Inserisci l'orario di entrata per avviare il timer.";
        } else {
            tick();
            if (exit === null) {
                setInterval(tick, 1000);
            }
        }
    }

    // ---------- Modifica voci inline ----------
    document.querySelectorAll(".edit-toggle").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var li = btn.closest(".entry");
            li.querySelector(".entry-view").hidden = true;
            var form = li.querySelector(".entry-edit");
            form.hidden = false;
            var input = form.querySelector("input");
            input.focus();
            input.setSelectionRange(input.value.length, input.value.length);
        });
    });

    document.querySelectorAll(".edit-cancel").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var li = btn.closest(".entry");
            li.querySelector(".entry-edit").hidden = true;
            li.querySelector(".entry-view").hidden = false;
        });
    });

    // ---------- Click su un task: copia il rapportino negli appunti ----------
    // Il link apre comunque il task in una nuova scheda (azione nativa dell'ancora).
    document.querySelectorAll(".task-id[data-clipboard]").forEach(function (link) {
        link.addEventListener("click", function () {
            var text = link.getAttribute("data-clipboard");
            if (navigator.clipboard && text) {
                navigator.clipboard.writeText(text).then(function () {
                    flashCopied(link);
                }).catch(function () { /* clipboard non disponibile (serve https o localhost) */ });
            }
        });
    });

    function flashCopied(link) {
        var badge = document.createElement("span");
        badge.className = "copied-badge";
        badge.textContent = "copiato ✓";
        link.parentNode.appendChild(badge);
        setTimeout(function () { badge.remove(); }, 1500);
    }

    // ---------- Selettore task: inserisce #codice come testo nell'input attivita' ----------
    var picker = document.querySelector(".task-picker");
    var promptInput = document.querySelector(".prompt-bar input[name='description']");
    if (picker && promptInput) {
        picker.addEventListener("change", function () {
            if (picker.value) {
                var cur = promptInput.value.replace(/\s+$/, "");
                promptInput.value = (cur ? cur + " " : "") + picker.value + " ";
                picker.value = "";
                promptInput.focus();
            }
        });
    }
})();
