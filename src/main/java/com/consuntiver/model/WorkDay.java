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

import java.time.Instant;
import java.time.LocalDate;

/**
 * La giornata lavorativa di un utente: quattro orari inseriti a mano
 * (entrata, inizio e fine pausa pranzo, uscita). Una riga per utente per giorno.
 * Gli orari sono in UTC; possono essere null finche' non vengono registrati.
 */
@Entity
@Table(name = "work_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_work_session_user_date",
                columnNames = {"id_user", "work_date"}))
public class WorkDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Il giorno (fuso Europe/Rome) a cui si riferiscono gli orari. */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** Orario di entrata: avvia il timer delle 8 ore. */
    @Column(name = "entry_at")
    private Instant entryAt;

    /** Orario di inizio pausa pranzo. */
    @Column(name = "lunch_start_at")
    private Instant lunchStartAt;

    /** Orario di rientro dalla pausa pranzo. */
    @Column(name = "lunch_end_at")
    private Instant lunchEndAt;

    /** Orario di uscita: chiude la giornata. */
    @Column(name = "exit_at")
    private Instant exitAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user")
    private User user;

    public WorkDay() {
    }

    public WorkDay(LocalDate workDate, User user) {
        this.workDate = workDate;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public Instant getEntryAt() {
        return entryAt;
    }

    public void setEntryAt(Instant entryAt) {
        this.entryAt = entryAt;
    }

    public Instant getLunchStartAt() {
        return lunchStartAt;
    }

    public void setLunchStartAt(Instant lunchStartAt) {
        this.lunchStartAt = lunchStartAt;
    }

    public Instant getLunchEndAt() {
        return lunchEndAt;
    }

    public void setLunchEndAt(Instant lunchEndAt) {
        this.lunchEndAt = lunchEndAt;
    }

    public Instant getExitAt() {
        return exitAt;
    }

    public void setExitAt(Instant exitAt) {
        this.exitAt = exitAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
