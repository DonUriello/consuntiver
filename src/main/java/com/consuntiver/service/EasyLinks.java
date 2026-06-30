package com.consuntiver.service;

/** Costruzione centralizzata dei link verso le issue di Easy. */
public final class EasyLinks {

    public static final String ISSUE_BASE_URL = "https://prd.galileonetwork.it/easy/issues/";

    private EasyLinks() {
    }

    /** Link alla issue Easy a partire dal numero del task (sempre senza '#'). */
    public static String issueUrl(String taskId) {
        return ISSUE_BASE_URL + taskId;
    }
}
