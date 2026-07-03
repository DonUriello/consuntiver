package com.consuntiver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un task riutilizzabile (per-utente, con anno di validita'). Puo' essere creato a mano
 * (task fisso) o automaticamente quando un'attivita' cita un #codice non ancora presente.
 */
@Entity
@Table(name = "task")
public class FixedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero del ticket sul portale (es. 129671), senza '#'. Facoltativo. */
    @Column(name = "cod_task", length = 50)
    private String taskNumber;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "year", nullable = false)
    private int year;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user")
    private User user;

    public FixedTask() {
    }

    public FixedTask(String taskNumber, String name, String description, int year, User user) {
        this.taskNumber = taskNumber;
        this.name = name;
        this.description = description;
        this.year = year;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskNumber() {
        return taskNumber;
    }

    public void setTaskNumber(String taskNumber) {
        this.taskNumber = taskNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
