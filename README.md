# Consuntiver

Piccola applicazione Spring Boot per tenere traccia di cosa stai facendo
durante la giornata. Quando vieni interrotto, scrivi nella barra in basso su
cosa stai lavorando: viene salvata una riga con data/ora e descrizione.
Lo storico della giornata appare nell'area sopra la barra.

## Funzionalita' (MVP)

- Registrazione utente (password cifrata con BCrypt)
- Login / Logout
- Pagina principale con:
  - storico delle voci **di oggi** (dalla piu' recente alla piu' vecchia)
  - barra di input in basso per registrare una nuova voce
- Ogni utente vede **solo** le proprie voci

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

## Note tecniche

- Gli orari sono salvati a DB in **UTC** (`Instant`) e mostrati nel fuso
  `Europe/Rome`. Il fuso usato per delimitare "la giornata" e' definito in
  `HomeController`.
- Lo schema viene creato/aggiornato automaticamente da Hibernate
  (`ddl-auto=update`). Per un uso reale conviene passare a migrazioni
  gestite (es. Flyway).
