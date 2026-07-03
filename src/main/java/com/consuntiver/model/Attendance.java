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

/**
 * Una sessione di presenza (timbratura): orario di entrata e (quando chiusa) di uscita.
 * clockOut null = sessione aperta ("in servizio"). Tutti gli istanti sono in UTC.
 */
@Entity
@Table(name = "work_session")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clock_in", nullable = false)
    private Instant clockIn;

    @Column(name = "clock_out")
    private Instant clockOut;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_user")
    private User user;

    public Attendance() {
    }

    public Attendance(Instant clockIn, User user) {
        this.clockIn = clockIn;
        this.user = user;
    }

    public boolean isOpen() {
        return clockOut == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getClockIn() {
        return clockIn;
    }

    public void setClockIn(Instant clockIn) {
        this.clockIn = clockIn;
    }

    public Instant getClockOut() {
        return clockOut;
    }

    public void setClockOut(Instant clockOut) {
        this.clockOut = clockOut;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
