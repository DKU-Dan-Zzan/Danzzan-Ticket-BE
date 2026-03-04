package com.danzzan.ticketing.domain.event.repository;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FestivalEventRepository extends JpaRepository<FestivalEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from FestivalEvent e where e.id = :eventId")
    Optional<FestivalEvent> findByIdForUpdate(@Param("eventId") Long eventId);
}
