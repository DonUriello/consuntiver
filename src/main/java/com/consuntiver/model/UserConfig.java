package com.consuntiver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Configurazione del sistema di consuntivazione, una per utente. */
@Entity
@Table(name = "user_config")
public class UserConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    /** Indirizzo della home del sistema (pulsante "Easy"). */
    @Column(name = "home_url", nullable = false, length = 500)
    private String homeUrl;

    /** URL base a cui accodare il numero del task per costruire il link. */
    @Column(name = "task_base_url", nullable = false, length = 500)
    private String taskBaseUrl;

    public UserConfig() {
    }

    public UserConfig(User user, String homeUrl, String taskBaseUrl) {
        this.user = user;
        this.homeUrl = homeUrl;
        this.taskBaseUrl = taskBaseUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHomeUrl() {
        return homeUrl;
    }

    public void setHomeUrl(String homeUrl) {
        this.homeUrl = homeUrl;
    }

    public String getTaskBaseUrl() {
        return taskBaseUrl;
    }

    public void setTaskBaseUrl(String taskBaseUrl) {
        this.taskBaseUrl = taskBaseUrl;
    }
}
