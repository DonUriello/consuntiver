# Consuntiver

Piccola applicazione Spring Boot per tenere traccia di cosa stai facendo
durante la giornata. Quando vieni interrotto, scrivi nella barra in basso su
cosa stai lavorando: viene salvata una riga con data/ora e descrizione.
Lo storico della giornata appare nell'area sopra la barra.

## Funzionalita'

- Registrazione utente (password cifrata con BCrypt)
- Login / Logout
- Pagina principale con:
  - storico delle voci **di oggi** (dalla piu' recente alla piu' vecchia)
  - barra di input in basso per registrare una nuova voce; all'inserimento la
    voce precedente ancora aperta viene chiusa in automatico (la sua fine
    diventa "adesso")
  - azioni su ogni voce: **Termina** l'attivita' in corso, **Modifica** il testo
    (ri-collega il task citato in caso di errore) ed **Elimina** la riga
  - **Orario di lavoro**: quattro orari inseriti a mano — **Entrata**, **Pausa
    pranzo**, **Rientro**, **Uscita** — salvati insieme con *Salva orari*
  - **Contatore live** che dall'entrata conta il **lavoro netto** (esclusa la
    pausa pranzo: quella effettiva con un minimo di 45 min, oppure 1 ora se non
    indicata) verso le **8 ore**, con barra di avanzamento e stima dell'uscita
    prevista
  - **Colonna task** a destra: rileva i numeri di task citati nelle righe
    (es. `#12345` o `123456`) e ne crea il link verso Easy
    (`.../easy/issues/<id>`), con la descrizione presa dalla riga.
    I numeri "nudi" sono riconosciuti come task solo da 5 cifre in su, per
    evitare falsi positivi (orari, anni); con il prefisso `#` basta una cifra.
  - Pagina **Storico giornate**: le giornate passate (orari con **lavoro netto**,
    totale del tempo e ripartizione per task, elenco delle attivita' svolte)
  - Pagina **Task fissi**: task validi per un intero anno (es. manutenzioni
    annuali), con numero task opzionale che genera il link a Easy (stessa
    logica della colonna laterale), raggruppati per anno e con cancellazione
  - Pagina **Impostazioni**: per utente si configurano l'indirizzo **Home** e
    l'**URL base** con cui si costruiscono i link ai task
- Ogni utente vede e modifica **solo** le proprie voci, orari e task fissi
- **Navigazione** (menu in alto): **Home**, il menu **Configurazioni**
  (Task fissi, Impostazioni) e il menu col **tuo nome** (Storico, Esci)

## Account demo

All'avvio è disponibile un account dimostrativo con una giornata di lavoro
già simulata (task d'esempio, orari, contatore):

- Username: `demo_galileo`
- Password: `death_earth`

In locale i suoi dati vengono rigenerati a ogni avvio (seeder, solo fuori dal profilo
`prod`). In produzione la demo si inizializza con [`docs/demo_galileo.sql`](docs/demo_galileo.sql).

> Guida d'uso completa: [`docs/GUIDA.md`](docs/GUIDA.md). In app è anche
> raggiungibile passando il mouse sull'icona **?** accanto al nome.

## Stack

- Java 21, Spring Boot 3
- Spring Web + Thymeleaf
- Spring Security
- Spring Data JPA — **H2** in locale (sviluppo), **PostgreSQL/Supabase** in produzione

## Database

- Schema: [`docs/schema-supabase.sql`](docs/schema-supabase.sql) (tabelle `users`,
  `task`, `activity`, `work_session`, `settings`).
- Inizializzazione demo su Supabase: [`docs/demo_galileo.sql`](docs/demo_galileo.sql).
- In produzione lo schema è gestito su Supabase (Hibernate lo **valida** soltanto,
  `ddl-auto=validate`); in locale su H2 viene generato dalle entità.

## Avvio

```bash
mvn spring-boot:run
```

Poi apri http://localhost:8080 — verrai rediretto al login. Registra un
account e accedi.

I dati vengono salvati nella cartella `./data` (esclusa da git).

### Console H2 (sviluppo)

Disponibile su http://localhost:8080/h2-console con:

- JDBC URL: `jdbc:h2:file:./data/consuntiver`
- User: `sa` — Password: *(vuota)*

## Deploy su Render

Il repo include un **Blueprint** (`render.yaml`) che crea il solo **web service**
(build via `Dockerfile`). Il database **non** è gestito da Render: è un progetto
**Supabase** esterno, a cui l'app si collega tramite variabili d'ambiente.

1. Vai su [render.com](https://render.com), accedi e collega il tuo account GitHub.
2. **New > Blueprint**, seleziona questo repository e il branch.
3. Render legge `render.yaml` e crea il servizio web con `SPRING_PROFILES_ACTIVE=prod`.
4. Imposta a mano, tra le variabili d'ambiente del servizio, le credenziali del
   datasource Supabase:
   - `SPRING_DATASOURCE_URL` — es. `jdbc:postgresql://<host-supabase>:5432/postgres?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` — es. `postgres.<project-ref>` (session pooler) o `postgres` (diretta)
   - `SPRING_DATASOURCE_PASSWORD` — la password del database Supabase

> Nota: il piano free di Render mette in pausa il servizio dopo un periodo di
> inattivita'; la prima richiesta dopo la pausa puo' impiegare qualche secondo.

### Locale vs produzione

- **Locale** (nessun profilo): usa **H2 su file** (`./data`). Nessuna configurazione.
- **Produzione** (profilo `prod`): usa **PostgreSQL su Supabase**. La connessione
  arriva interamente da `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` /
  `SPRING_DATASOURCE_PASSWORD`. Va usato il **session pooler** (porta 5432) o la
  connessione **diretta**, non il *transaction pooler* (6543), incompatibile con
  i prepared statement di Hibernate.

## Note tecniche

- Gli orari sono salvati a DB in **UTC** (`Instant`) e mostrati nel fuso
  `Europe/Rome`. Il fuso usato per delimitare "la giornata" e' definito in
  `HomeController`.
- Schema: in locale (H2) è generato da Hibernate (`ddl-auto=update`); in
  produzione è gestito su Supabase e Hibernate lo **valida** soltanto
  (`ddl-auto=validate`), vedi [`docs/schema-supabase.sql`](docs/schema-supabase.sql).
  Per un uso reale conviene passare a migrazioni gestite (es. Flyway).
- Il pool di connessioni (HikariCP) in `prod` è tenuto piccolo
  (`maximum-pool-size=5`): il *session pooler* di Supabase limita i client totali
  a `pool_size` (di default 15), quindi un pool grande — sommato all'overlap di un
  redeploy — lo saturerebbe (`EMAXCONNSESSION`).
