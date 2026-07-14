package com.consuntiver.service;

import com.consuntiver.service.EasyScraperService.EasyIssue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test del parsing (la parte fragile e dipendente dall'HTML). Il login e il fetch
 * fanno rete e non sono testabili offline: qui si verifica solo l'estrazione dei
 * campi da una pagina "tipo Redmine" salvata come fixture.
 */
class EasyScraperServiceTest {

    private final EasyScraperService service = new EasyScraperService();

    private Document loadFixture(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in).as("fixture presente: " + path).isNotNull();
            return Jsoup.parse(in, "UTF-8", "https://prd.galileonetwork.it/easy/");
        }
    }

    @Test
    void estraeTitoloEDescrizioneDaUnaIssueTipoRedmine() throws Exception {
        Document doc = loadFixture("/easy/issue-sample.html");

        EasyIssue issue = service.parseIssue("119971", doc);

        assertThat(issue.id()).isEqualTo("119971");
        assertThat(issue.subject())
                .isEqualTo("Aggiunta id_riga e id_lock alla tabella SocietaModuloFunzione");
        assertThat(issue.description()).contains("Descrizione di esempio del task");
    }

    @Test
    void restituisceCampiVuotiSeLaStrutturaNonCorrisponde() {
        // Pagina senza i selettori attesi: niente eccezioni, solo stringhe vuote.
        Document doc = Jsoup.parse("<html><body><p>pagina non riconosciuta</p></body></html>");

        EasyIssue issue = service.parseIssue("42", doc);

        assertThat(issue.id()).isEqualTo("42");
        assertThat(issue.subject()).isEmpty();
        assertThat(issue.description()).isEmpty();
    }
}
