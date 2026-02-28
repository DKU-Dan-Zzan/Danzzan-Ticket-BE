package com.danzzan.ticketing.global.config;

import com.danzzan.ticketing.domain.event.model.entity.FestivalEvent;
import com.danzzan.ticketing.domain.event.model.entity.TicketingStatus;
import com.danzzan.ticketing.domain.event.repository.FestivalEventRepository;
import com.danzzan.ticketing.domain.ticket.repository.TicketRedisRepository;
import com.danzzan.ticketing.domain.ticket.repository.UserTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventSyncInitializer {

    private final FestivalEventRepository festivalEventRepository;
    private final UserTicketRepository userTicketRepository;
    private final TicketRedisRepository ticketRedisRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOpenEventsToRedis() {
        List<FestivalEvent> allEvents = festivalEventRepository.findAll();

        for (FestivalEvent event : allEvents) {
            if (event.getTicketingStatus() == TicketingStatus.OPEN) {
                long sold = userTicketRepository.countByEventId(event.getId());
                int remaining = event.getTotalCapacity() - (int) sold;
                ticketRedisRepository.openEvent(event.getId(), remaining);
                log.info("[Redis Sync] 이벤트 {} '{}' → OPEN, 잔여석: {}", event.getId(), event.getTitle(), remaining);
            }
        }

        log.info("[Redis Sync] 서버 시작 시 OPEN 이벤트 Redis 동기화 완료");
    }
}
