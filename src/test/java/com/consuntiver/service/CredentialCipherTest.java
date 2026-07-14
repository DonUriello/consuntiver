package com.consuntiver.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialCipherTest {

    private final CredentialCipher cipher = new CredentialCipher();

    @Test
    void cifraturaEDecifraturaConLaStessaPasswordTornano() {
        String enc = cipher.encrypt("mario.rossi", "password-app-utente");
        assertThat(cipher.decrypt(enc, "password-app-utente")).isEqualTo("mario.rossi");
    }

    @Test
    void gestisceCaratteriNonAsciiEStringheLunghe() {
        String segreto = "P@ssw0rd! àèìòù — 123456 çøßñ";
        String enc = cipher.encrypt(segreto, "chiave con spazi e simboli #!$");
        assertThat(cipher.decrypt(enc, "chiave con spazi e simboli #!$")).isEqualTo(segreto);
    }

    @Test
    void conPasswordSbagliataLaDecifraturaFallisce() {
        String enc = cipher.encrypt("segreto", "password-giusta");
        assertThatThrownBy(() -> cipher.decrypt(enc, "password-sbagliata"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ogniCifraturaProduceUnTokenDiverso() {
        // Salt e IV casuali: due cifrature dello stesso testo non coincidono.
        assertThat(cipher.encrypt("stesso", "pw")).isNotEqualTo(cipher.encrypt("stesso", "pw"));
    }

    @Test
    void ilTokenHaIlFormatoAtteso() {
        String enc = cipher.encrypt("x", "pw");
        assertThat(enc).startsWith("v1:");
        assertThat(enc.split(":")).hasSize(4);
    }

    @Test
    void tokenMalformatoVieneRifiutato() {
        assertThatThrownBy(() -> cipher.decrypt("non-un-token", "pw"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
