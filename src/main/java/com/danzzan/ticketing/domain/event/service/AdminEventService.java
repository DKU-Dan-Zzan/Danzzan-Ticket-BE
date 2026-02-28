package com.danzzan.ticketing.domain.event.service;

import com.danzzan.ticketing.domain.event.dto.EventListResponseDTO;
import com.danzzan.ticketing.domain.event.dto.EventStatsResponseDTO;

public interface AdminEventService {
    EventListResponseDTO listEvents();
    EventStatsResponseDTO getEventStats(Long eventId);
    void openEvent(Long eventId);
    void closeEvent(Long eventId);
}
