-- ============================================================================
-- Consuntiver - inizializzazione utente demo "demo_galileo"
-- Da eseguire su Supabase (SQL editor) DOPO aver creato lo schema
-- (docs/schema-supabase.sql). Lo script e' ri-eseguibile: rigenera la giornata
-- demo (attivita', timbrature, task) ancorata a "oggi".
--   username: demo_galileo   password: death_earth
-- ============================================================================

-- 1) Utente (la password e' l'hash BCrypt di "death_earth")
INSERT INTO users (username, password, email)
VALUES ('demo_galileo', '$2a$10$bFkpoEOmTyqQyXQIYkC2ROnEnflPU53SPmeRMw/NVkLGPc2Py537e', NULL)
ON CONFLICT (username) DO UPDATE SET password = EXCLUDED.password;

-- 2) Pulizia dei dati demo (per rendere lo script ri-eseguibile)
DELETE FROM activity     WHERE id_user = (SELECT id FROM users WHERE username = 'demo_galileo');
DELETE FROM work_session WHERE id_user = (SELECT id FROM users WHERE username = 'demo_galileo');
DELETE FROM task         WHERE id_user = (SELECT id FROM users WHERE username = 'demo_galileo');

-- 3) Impostazioni (URL Galileo)
INSERT INTO settings (id_user, cod_settings, value)
SELECT u.id, s.code, s.val
FROM users u
CROSS JOIN (VALUES
    ('home_url',      'https://prd.galileonetwork.it/easy/'),
    ('task_base_url', 'https://prd.galileonetwork.it/easy/issues/')
) AS s(code, val)
WHERE u.username = 'demo_galileo'
ON CONFLICT (id_user, cod_settings) DO UPDATE SET value = EXCLUDED.value;

-- 4) Task riutilizzabili (3 ticket reali, anno corrente)
INSERT INTO task (id_user, cod_task, name, description, year)
SELECT u.id, t.cod, t.name, NULL, EXTRACT(YEAR FROM CURRENT_DATE)::int
FROM users u
CROSS JOIN (VALUES
    ('129671', 'Analisi e sviluppo'),
    ('119971', 'Bug e refactoring'),
    ('119995', 'Documentazione e deploy')
) AS t(cod, name)
WHERE u.username = 'demo_galileo';

-- 5) Attivita' della giornata (08:00 -> 17:00 di oggi, fuso Europe/Rome),
--    collegate ai task tramite il codice.
INSERT INTO activity (id_user, id_task, start_at, end_at, description)
SELECT u.id,
       tk.id,
       (CURRENT_DATE + a.s) AT TIME ZONE 'Europe/Rome',
       (CURRENT_DATE + a.e) AT TIME ZONE 'Europe/Rome',
       a.descr
FROM users u
CROSS JOIN (VALUES
    (TIME '08:00', TIME '09:00', '129671', '#129671 analisi requisiti'),
    (TIME '09:00', TIME '09:45', '119971', '#119971 fix bug login'),
    (TIME '09:45', TIME '11:00', '129671', '#129671 sviluppo nuova feature'),
    (TIME '11:00', TIME '11:40', '119995', '#119995 review della pull request'),
    (TIME '11:40', TIME '12:30', '119971', '#119971 riunione di team'),
    (TIME '12:30', TIME '13:00', '129671', '#129671 correzioni post review'),
    (TIME '13:00', TIME '14:00', NULL,     'Pausa pranzo'),
    (TIME '14:00', TIME '15:00', '119995', '#119995 stesura documentazione'),
    (TIME '15:00', TIME '15:45', '119971', '#119971 refactoring'),
    (TIME '15:45', TIME '16:20', '119995', '#119995 deploy in staging'),
    (TIME '16:20', TIME '17:00', '129671', '#129671 aggiornamento ticket')
) AS a(s, e, cod, descr)
LEFT JOIN task tk
       ON tk.id_user = u.id
      AND tk.cod_task = a.cod
      AND tk.year = EXTRACT(YEAR FROM CURRENT_DATE)::int
WHERE u.username = 'demo_galileo';

-- 6) Timbrature: mattina 08:00 -> 13:00, pomeriggio 14:00 -> 17:00 (8 ore)
INSERT INTO work_session (id_user, clock_in, clock_out)
SELECT u.id,
       (CURRENT_DATE + w.s) AT TIME ZONE 'Europe/Rome',
       (CURRENT_DATE + w.e) AT TIME ZONE 'Europe/Rome'
FROM users u
CROSS JOIN (VALUES
    (TIME '08:00', TIME '13:00'),
    (TIME '14:00', TIME '17:00')
) AS w(s, e)
WHERE u.username = 'demo_galileo';
