package com.consuntiver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Un'attivita' (riga di log): interruzione giornaliera con inizio, fine ed eventuale task. */
@Entity
@Table(name = "activity")
public class WorkEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Inizio dell'attivita' (scritto alla creazione). UTC. */
    @Column(name = "start_at", nullable = false)
    private Instant startedAt;

    /** Fine dell'attivita' (valorizzata all'inserimento della successiva). Null = in corso. */
    @Column(name = "end_at")
    private Instant endedAt;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user")
    private User user;

    /** Task collegato (facoltativo: null per righe senza task, es. "Pausa pranzo"). */
    @ManyToOne
    @JoinColumn(name = "id_task")
    private FixedTask task;

    public WorkEntry() {
    }

    public WorkEntry(Instant startedAt, String description, User user) {
        this.startedAt = startedAt;
        this.description = description;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FixedTask getTask() {
        return task;
    }

    public void setTask(FixedTask task) {
        this.task = task;
    }
}
