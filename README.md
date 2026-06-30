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
  - barra di input in basso per registrare una nuova voce
  - **Modifica** del testo di ogni voce gia' inserita (in caso di errore)
  - **Entrata / Uscita**: timbrature dell'orario di lavoro (supporta piu' coppie
    al giorno, es. pausa pranzo)
  - **Contatore live** che avanza ogni secondo mentre sei "in servizio", somma le
    sessioni della giornata, mostra una barra di avanzamento e ti avvisa al
    raggiungimento delle **8 ore**
  - Pulsante **Easy** che apre https://prd.galileonetwork.it/easy/
  - **Colonna task** a destra: rileva i numeri di task citati nelle righe
    (es. `#12345` o `123456`) e ne crea il link verso Easy
    (`.../easy/issues/<id>`), con la descrizione presa dalla riga.
    I numeri "nudi" sono riconosciuti come task solo da 5 cifre in su, per
    evitare falsi positivi (orari, anni); con il prefisso `#` basta una cifra.
- Ogni utente vede e modifica **solo** le proprie voci e timbrature

## Stack

- Java 21, Spring Boot 3
- Spring Web + Thymeleaf
- Spring Security
- Spring Data JPA + H2 (database su file, persistente tra i riavvii)

## Avvio

```bash
./mvnw spring-boot:run     # oppure: mvn spring-boot:run
```

Poi apri http://localhost:8080 — verrai rediretto al login. Registra un
account e accedi.

I dati vengono salvati nella cartella `./data` (esclusa da git).

### Console H2 (sviluppo)

Disponibile su http://localhost:8080/h2-console con:

- JDBC URL: `jdbc:h2:file:./data/consuntiver`
- User: `sa` — Password: *(vuota)*

## Deploy su Render

Il repo include un **Blueprint** (`render.yaml`) che crea automaticamente sia il
web service (build via `Dockerfile`) sia un database **PostgreSQL** gestito.

1. Vai su [render.com](https://render.com), accedi e collega il tuo account GitHub.
2. **New > Blueprint**, seleziona questo repository e il branch.
3. Render legge `render.yaml`, crea il database `consuntiver-db` e il servizio web,
   e collega in automatico le credenziali del DB tramite variabili d'ambiente.
4. Al primo deploy l'app parte col profilo `prod` su PostgreSQL.

> Nota: il piano free di Render mette in pausa il servizio dopo un periodo di
> inattivita'; la prima richiesta dopo la pausa puo' impiegare qualche secondo.

### Locale vs produzione

- **Locale** (nessun profilo): usa **H2 su file** (`./data`). Nessuna configurazione.
- **Produzione** (profilo `prod`): usa **PostgreSQL**. L'URL viene costruito dalle
  variabili `DB_HOST`, `DB_PORT`, `DB_NAME`, piu' `SPRING_DATASOURCE_USERNAME` e
  `SPRING_DATASOURCE_PASSWORD`, tutte iniettate da Render.

## Note tecniche

- Gli orari sono salvati a DB in **UTC** (`Instant`) e mostrati nel fuso
  `Europe/Rome`. Il fuso usato per delimitare "la giornata" e' definito in
  `HomeController`.
- Lo schema viene creato/aggiornato automaticamente da Hibernate
  (`ddl-auto=update`). Per un uso reale conviene passare a migrazioni
  gestite (es. Flyway).
