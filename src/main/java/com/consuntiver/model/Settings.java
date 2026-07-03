package com.consuntiver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Impostazione per-utente in formato chiave/valore (es. home_url, task_base_url). */
@Entity
@Table(name = "settings", uniqueConstraints = @UniqueConstraint(columnNames = {"id_user", "cod_settings"}))
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user")
    private User user;

    @Column(name = "cod_settings", nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 500)
    private String value;

    public Settings() {
    }

    public Settings(User user, String code, String value) {
        this.user = user;
        this.code = code;
        this.value = value;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
