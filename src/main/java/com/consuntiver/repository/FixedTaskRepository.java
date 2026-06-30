package com.consuntiver.repository;

import com.consuntiver.model.FixedTask;
import com.consuntiver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FixedTaskRepository extends JpaRepository<FixedTask, Long> {

    /** Task fissi dell'utente, anni piu' recenti prima e, a parita' di anno, i piu' nuovi prima. */
    List<FixedTask> findByUserOrderByYearDescIdDesc(User user);
}
