package com.consuntiver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Un task "fisso" valido per un intero anno (es. una manutenzione annuale).
 * Il numero del task e' opzionale: se presente serve a costruire il link verso Easy.
 */
@Entity
@Table(name = "fixed_tasks")
public class FixedTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero del task su Easy (solo cifre, senza '#'). Puo' essere null. */
    @Column(name = "task_number")
    private String taskNumber;

    @Column(nullable = false, length = 1000)
    private String description;

    /** Anno di validita' del task. Nome colonna esplicito per evitare parole riservate. */
    @Column(name = "task_year", nullable = false)
    private int year;

    @ManyToOne(optional = false)
    private User user;

    public FixedTask() {
    }

    public FixedTask(String taskNumber, String description, int year, User user) {
        this.taskNumber = taskNumber;
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
