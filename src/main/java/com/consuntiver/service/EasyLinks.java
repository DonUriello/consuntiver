package com.consuntiver.service;

/** Valori di default e costruzione dei link verso Easy (l'URL base e' configurabile per utente). */
public final class EasyLinks {

    /** Home di default del sistema di consuntivazione. */
    public static final String DEFAULT_HOME_URL = "https://prd.galileonetwork.it/easy/";

    /** URL base di default per costruire il link a una issue (vi si accoda il numero del task). */
    public static final String DEFAULT_ISSUE_BASE_URL = "https://prd.galileonetwork.it/easy/issues/";

    private EasyLinks() {
    }

    /** Link alla issue: URL base (configurato) + numero del task (sempre senza '#'). */
    public static String issueUrl(String taskBaseUrl, String taskId) {
        return taskBaseUrl + taskId;
    }
}
