(function () {
    "use strict";

    // ---------- Contatore live del tempo di lavoro ----------
    var el = document.getElementById("worktime");
    if (el) {
        var accumulated = parseInt(el.dataset.accumulated || "0", 10);
        var openSinceRaw = el.dataset.openSince;
        var openSince = openSinceRaw ? parseInt(openSinceRaw, 10) : null;
        var target = parseInt(el.dataset.target || "28800", 10);
        var serverNow = parseInt(el.dataset.serverNow || Date.now(), 10);
        // Differenza tra orologio del server e del browser, per non sballare il conteggio.
        var offset = serverNow - Date.now();

        var counterEl = document.getElementById("wt-counter");
        var barEl = document.getElementById("wt-bar");
        var statusEl = document.getElementById("wt-status");

        var fmt = function (totalSeconds) {
            var s = Math.max(0, Math.floor(totalSeconds));
            var h = Math.floor(s / 3600);
            var m = Math.floor((s % 3600) / 60);
            var sec = s % 60;
            var pad = function (n) { return n < 10 ? "0" + n : "" + n; };
            return pad(h) + ":" + pad(m) + ":" + pad(sec);
        };

        var tick = function () {
            var total = accumulated;
            if (openSince !== null) {
                total += (Date.now() + offset - openSince) / 1000;
            }
            counterEl.textContent = fmt(total);

            var pct = Math.min(100, (total / target) * 100);
            barEl.style.width = pct.toFixed(1) + "%";

            if (total >= target) {
                barEl.classList.add("done");
                statusEl.textContent = "✅ Hai completato le 8 ore!";
            } else if (openSince !== null) {
                statusEl.textContent = "Mancano " + fmt(target - total) + " alle 8 ore";
            } else {
                statusEl.textContent = "Non sei in servizio";
            }
        };

        tick();
        if (openSince !== null) {
            setInterval(tick, 1000);
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
