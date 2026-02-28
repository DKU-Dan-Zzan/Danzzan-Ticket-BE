package com.danzzan.ticketing.domain.event.service;

import com.danzzan.ticketing.domain.event.dto.EventListResponseDTO;
import com.danzzan.ticketing.domain.event.dto.EventStatsResponseDTO;
import com.danzzan.ticketing.domain.event.dto.EventSummaryDTO;
import com.danzzan.ticketing.domain.event.exception.EventNotFoundException;
import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.event.model.entity.TicketingStatus;
import com.danzzan.ticketing.domain.event.repository.FestivalEventRepository;
import com.danzzan.ticketing.domain.ticket.model.entity.TicketStatus;
import com.danzzan.ticketing.domain.ticket.repository.TicketRedisRepository;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {

    private final FestivalEventRepository festivalEventRepository;
    private final UserTicketRepository userTicketRepository;
    private final TicketRedisRepository ticketRedisRepository;

    @Override
    public EventListResponseDTO listEvents() {
        List<FestivalEvent> events = festivalEventRepository.findAll(
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        Map<LocalDate, Integer> dayIndex = buildDayIndex(events);

        List<EventSummaryDTO> summaries = events.stream()
                .map(event -> EventSummaryDTO.builder()
                        .eventId(event.getId())
                        .title(event.getTitle())
                        .dayLabel("DAY " + dayIndex.get(event.getEventDate()))
                        .eventDate(event.getEventDate().toString())
                        .ticketingStatus(event.getTicketingStatus())
                        .totalCapacity(event.getTotalCapacity())
                        .build())
                .toList();

        return EventListResponseDTO.builder()
                .events(summaries)
                .build();
    }

    @Override
    public EventStatsResponseDTO getEventStats(Long eventId) {
        FestivalEvent event = festivalEventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        long totalTickets = userTicketRepository.countByEventId(eventId);
        long ticketsConfirmed = userTicketRepository.countByEventIdAndStatus(eventId, TicketStatus.CONFIRMED);
        long ticketsIssued = userTicketRepository.countByEventIdAndStatus(eventId, TicketStatus.ISSUED);

        int totalCapacity = event.getTotalCapacity();
        int remainingCapacity = Math.max(0, totalCapacity - (int) totalTickets);
        double issueRate = totalTickets == 0 ? 0.0 : ((double) ticketsIssued / totalTickets) * 100.0;

        return EventStatsResponseDTO.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .eventDate(event.getEventDate().toString())
                .totalCapacity(totalCapacity)
                .totalTickets(totalTickets)
                .ticketsConfirmed(ticketsConfirmed)
                .ticketsIssued(ticketsIssued)
                .issueRate(issueRate)
                .remainingCapacity(remainingCapacity)
                .build();
    }

    @Override
    @Transactional
    public void openEvent(Long eventId) {
        FestivalEvent event = festivalEventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        if (event.getTicketingStatus() != TicketingStatus.READY) {
            throw new IllegalStateException("READY 상태의 이벤트만 오픈할 수 있습니다. 현재 상태: " + event.getTicketingStatus());
        }

        // Redis에 잔여수량 + 상태 SET
        ticketRedisRepository.openEvent(eventId, event.getTotalCapacity());

        // DB 상태 변경
        event.changeStatus(TicketingStatus.OPEN);
        festivalEventRepository.save(event);
    }

    @Override
    @Transactional
    public void closeEvent(Long eventId) {
        FestivalEvent event = festivalEventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        if (event.getTicketingStatus() != TicketingStatus.OPEN) {
            throw new IllegalStateException("OPEN 상태의 이벤트만 마감할 수 있습니다. 현재 상태: " + event.getTicketingStatus());
        }

        // Redis 상태 CLOSED
        ticketRedisRepository.closeEvent(eventId);

        // DB 상태 변경
        event.changeStatus(TicketingStatus.CLOSED);
        festivalEventRepository.save(event);
    }

    private Map<LocalDate, Integer> buildDayIndex(List<FestivalEvent> events) {
        Map<LocalDate, Integer> indexByDate = new LinkedHashMap<>();
        int idx = 1;
        for (FestivalEvent event : events) {
            LocalDate date = event.getEventDate();
            if (!indexByDate.containsKey(date)) {
                indexByDate.put(date, idx++);
            }
        }
        return indexByDate;
    }
}
