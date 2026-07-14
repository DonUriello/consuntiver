package com.consuntiver.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Scraping del portale Easy (Easy Redmine/Project) tramite Jsoup: esegue il login
 * con le credenziali dell'utente, scarica la pagina di una issue e ne estrae i
 * campi (titolo, descrizione).
 *
 * <p>Perche' Jsoup e non un browser headless (Selenium/Playwright): l'app gira su
 * Render free (~512 MB) e un Chrome headless non ci starebbe. Jsoup e' una semplice
 * libreria (fetch + parser HTML), senza binari esterni.
 *
 * <p><b>Limiti noti</b>:
 * <ul>
 *   <li>Funziona se il login e' un normale form POST (nessun 2FA / SSO / captcha).</li>
 *   <li>I selettori del parsing seguono lo standard Redmine e vanno <b>calibrati
 *       sull'HTML reale di Easy</b> (vedi {@link #parseIssue}).</li>
 *   <li>Lo scraping e' fragile: cambi di tema/versione del portale possono romperlo.</li>
 * </ul>
 */
@Service
public class EasyScraperService {

    private static final int TIMEOUT_MS = 15_000;
    private static final String USER_AGENT = "FocusShield/1.0 (task sync)";

    /** Sessione autenticata su Easy: i cookie da riusare nelle richieste successive. */
    public record EasySession(Map<String, String> cookies) {
    }

    /** Dati estratti dalla pagina di una issue di Easy. */
    public record EasyIssue(String id, String subject, String description) {
    }

    /**
     * Esegue il login su Easy. Scarica prima la pagina di login per recuperare il
     * token CSRF (Rails {@code authenticity_token}), poi invia le credenziali e
     * restituisce i cookie di sessione.
     *
     * <p>I nomi dei campi seguono lo standard Redmine ({@code username},
     * {@code password}, {@code authenticity_token}); se l'HTML reale di Easy usa nomi
     * diversi vanno adeguati qui.
     *
     * @throws IOException se la rete fallisce o il login viene rifiutato
     */
    public EasySession login(String baseUrl, String username, String password) throws IOException {
        String loginUrl = join(baseUrl, "login");

        // 1) GET della pagina di login: cookie iniziali + token CSRF.
        Connection.Response get = Jsoup.connect(loginUrl)
                .method(Connection.Method.GET)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .execute();
        String token = get.parse().select("input[name=authenticity_token]").val();

        // 2) POST delle credenziali, riusando i cookie del GET.
        Connection.Response post = Jsoup.connect(loginUrl)
                .method(Connection.Method.POST)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .cookies(get.cookies())
                .data("authenticity_token", token)
                .data("username", username)
                .data("password", password)
                .data("login", "Login")
                .followRedirects(true)
                .execute();

        // In Redmine il login fallito ripresenta la pagina con un flash di errore.
        if (post.parse().selectFirst("#flash_error, div.flash.error, .flash.error") != null) {
            throw new IOException("Login a Easy fallito: credenziali non valide o account bloccato.");
        }
        return new EasySession(post.cookies());
    }

    /**
     * Scarica e interpreta la pagina di una issue usando una sessione gia' autenticata.
     *
     * @return i dati della issue, o vuoto se la pagina non e' accessibile (es. 404/403)
     * @throws IOException se la rete fallisce
     */
    public Optional<EasyIssue> fetchIssue(EasySession session, String baseUrl, String issueId) throws IOException {
        String url = join(baseUrl, "issues/" + issueId);
        Connection.Response resp = Jsoup.connect(url)
                .method(Connection.Method.GET)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .cookies(session.cookies())
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .execute();
        if (resp.statusCode() != 200) {
            return Optional.empty();
        }
        Document doc = resp.parse();
        // Se il portale ci ha rimandato al login, la sessione non e' valida.
        if (doc.selectFirst("#login-form, form[action$=login]") != null && doc.selectFirst(".issue") == null) {
            return Optional.empty();
        }
        return Optional.of(parseIssue(issueId, doc));
    }

    /**
     * Estrae i campi dalla pagina di una issue. I selettori sono quelli tipici di
     * Redmine:
     * <ul>
     *   <li>titolo: {@code .issue .subject h3} (in alcune versioni {@code h2});</li>
     *   <li>descrizione: {@code .issue .description .wiki}.</li>
     * </ul>
     * <b>Da calibrare sull'HTML reale di Easy</b>: e' l'unico punto che dipende dalla
     * struttura della pagina, isolato apposta per essere aggiustato senza toccare il
     * resto. Package-private per i test.
     */
    EasyIssue parseIssue(String issueId, Document doc) {
        String subject = firstText(doc,
                ".issue .subject h3",
                ".subject h3",
                "h2.issue-subject",
                "#content h2");
        String description = firstText(doc,
                ".issue .description .wiki",
                ".description .wiki",
                ".wiki.wiki-content");
        return new EasyIssue(issueId, subject, description);
    }

    /** Primo selettore che trova un elemento con testo non vuoto; "" se nessuno. */
    private static String firstText(Document doc, String... selectors) {
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null) {
                String text = el.text().trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    /** Concatena base e path gestendo lo slash finale. */
    private static String join(String base, String path) {
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("URL base di Easy mancante");
        }
        return base.endsWith("/") ? base + path : base + "/" + path;
    }
}
