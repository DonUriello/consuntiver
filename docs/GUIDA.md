# Guida a Consuntiver

Consuntiver ti aiuta ad annotare **al volo** cosa stai facendo durante la
giornata: quando vieni interrotto, scrivi su cosa stai lavorando e l'app salva
una riga con orario e descrizione. È pensata per chi viene interrotto spesso e
vuole ricostruire a fine giornata dove è andato il tempo.

## Accesso

- **Registrazione**: crea un account con username e password (la password è
  salvata cifrata).
- **Login / Logout**: ogni utente vede e modifica **solo** i propri dati.

## La pagina principale

### Barra di inserimento (in basso)
Scrivi cosa stai facendo e premi **Registra** (o **Invio**): viene salvata una
riga con data/ora e descrizione. Il campo riparte vuoto e pronto per la
prossima annotazione.

### Storico "Oggi"
Sopra la barra vedi le righe della giornata, dalla più recente alla più vecchia.
Ogni riga mostra **data e ora di inizio → fine**: l'inizio è quando l'hai scritta,
la fine viene registrata quando inserisci la riga successiva (la riga in corso
resta con `→ …`). Il pulsante **Modifica** apre il campo di modifica **solo**
quando lo premi (non è più sempre visibile).

### Orario di lavoro (in alto)
Quattro orari che inserisci a mano e poi salvi con **Salva orari**:
- **Entrata**: avvia il timer delle 8 ore.
- **Pausa pranzo** e **Rientro**: delimitano la pausa; il tempo tra i due non
  conta come lavoro. La pausa vale comunque **almeno 45 minuti**: se rientri
  prima, contano lo stesso 45 minuti.
- **Uscita**: chiude la giornata.

Un **contatore live** parte dall'orario di entrata e conta il **lavoro netto**
(esclusa la pausa pranzo) verso le **8 ore**, con barra di avanzamento. La pausa
è quella effettiva (minimo 45 min) se hai indicato sia pausa che rientro,
altrimenti si assume **1 ora**; in base a questo il timer stima anche l'orario
di uscita previsto.

### Task e link a Easy (colonna a destra)
Se in una riga citi il numero di un task, l'app lo riconosce e a destra crea il
link verso Easy, con accanto il **totale del tempo dedicato a quel task**. Gli
indirizzi (home e base dei link) si impostano in **Impostazioni**, per utente.
Formato del link: `<url base configurato><numero>`:
- `#12345` → riconosciuto sempre (con il cancelletto basta una cifra);
- `123456` → numero "nudo" riconosciuto da **5 cifre** in su (così non scambia
  per task orari o anni).

### Bottone tempo per argomento (azzurro)
Le righe che riguardano lo **stesso task** vengono raggruppate. Sulla riga più
recente di quel task compare un bottone azzurro tipo **`+0,75 (0:47)`**:
- il numero (`+0,75`) è il tempo dedicato a quell'argomento arrotondato **per
  eccesso** al quarto d'ora (minimo `+0,25`);
- tra parentesi (`0:47`) c'è il tempo **effettivo** non arrotondato.

Le righe con lo **stesso task oppure lo stesso testo** fanno parte dello stesso
argomento. Il tempo di ogni riga è `fine − inizio`; la riga in corso conta fino
ad adesso. I tempi vengono sommati e il totale compare **solo sulla riga più
recente** dell'argomento (le righe più vecchie non mostrano il tempo). Il
conteggio si **ri-adegua automaticamente** se correggi una riga. Il link del
bottone è ancora da definire.

Sotto la colonna dei task un riquadro mostra il **totale del tempo** della
giornata, aggiornato a ogni nuova rilevazione.

## Task fissi (pagina dedicata)
Dal link **Task fissi** nell'header gestisci i task validi per un intero anno
(es. manutenzioni annuali): indichi anno, numero task (opzionale, genera il link
a Easy) e descrizione. Sono raggruppati per anno e si possono eliminare.

## Pulsante Easy
Apre il gestionale Easy in una nuova scheda.

## Note tecniche rapide
- Gli orari sono salvati in UTC e mostrati nel fuso `Europe/Rome`.
- "La giornata" è delimitata sul fuso italiano; le righe passate restano a DB.
- In locale i dati stanno su H2 (file); in produzione su PostgreSQL.
