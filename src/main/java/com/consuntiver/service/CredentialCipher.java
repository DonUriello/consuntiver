package com.consuntiver.service;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Cifratura simmetrica di segreti brevi (le credenziali Easy dell'utente) usando
 * come passphrase la <b>password applicativa dell'utente</b>.
 *
 * <p>Schema: AES-256-GCM; la chiave deriva dalla passphrase con PBKDF2WithHmacSHA256
 * e un salt casuale <i>per valore</i>. Il token prodotto e' autodescrittivo:
 * <pre>v1:base64(salt):base64(iv):base64(ciphertext+tag)</pre>
 * cosi' ogni valore memorizzato porta con se' salt e IV e non serve uno store
 * separato. Il tag GCM garantisce che una passphrase errata (o un dato manomesso)
 * faccia fallire la decifratura invece di restituire spazzatura.
 *
 * <p>Nota: la passphrase e' la password in chiaro dell'utente, disponibile solo al
 * login (la password a DB e' un hash BCrypt, non reversibile). Chi usa questa classe
 * deve quindi procurarsi la passphrase al momento giusto (vedi integrazione con la
 * sessione).
 */
@Component
public class CredentialCipher {

    private static final String VERSION = "v1";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    /** Cifra {@code plaintext} con la passphrase; restituisce un token autodescrittivo. */
    public String encrypt(String plaintext, String passphrase) {
        try {
            byte[] salt = randomBytes(SALT_BYTES);
            byte[] iv = randomBytes(IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return String.join(":", VERSION, b64(salt), b64(iv), b64(ciphertext));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cifratura delle credenziali fallita", e);
        }
    }

    /**
     * Decifra un token prodotto da {@link #encrypt}.
     *
     * @throws IllegalArgumentException se la passphrase e' errata, il dato e' corrotto
     *                                  o il formato non e' riconosciuto
     */
    public String decrypt(String token, String passphrase) {
        String[] parts = token == null ? new String[0] : token.split(":");
        if (parts.length != 4 || !VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("Formato del token cifrato non valido");
        }
        try {
            byte[] salt = unb64(parts[1]);
            byte[] iv = unb64(parts[2]);
            byte[] ciphertext = unb64(parts[3]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Tag GCM non valido: passphrase errata o dato manomesso.
            throw new IllegalArgumentException("Decifratura fallita: password errata o dato corrotto", e);
        }
    }

    private SecretKeySpec deriveKey(String passphrase, byte[] salt) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        byte[] keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return b;
    }

    private static String b64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    private static byte[] unb64(String s) {
        return Base64.getDecoder().decode(s);
    }
}
