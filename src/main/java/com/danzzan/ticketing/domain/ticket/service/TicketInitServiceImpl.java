package com.danzzan.ticketing.domain.ticket.service;

import com.danzzan.ticketing.domain.ticket.dto.AdminTicketInitResponseDTO;
import com.danzzan.ticketing.domain.ticket.redis.TicketRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketInitServiceImpl implements TicketInitService {

    private static final String STOCK_SUFFIX = ":stock";
    private static final long SCAN_COUNT = 500L;
    private static final int UNLINK_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;

    @Override
    public AdminTicketInitResponseDTO initStock(String eventId, Long stock) {
        String stockKey = TicketRedisKeys.stockKey(eventId);
        String eventPrefix = eventPrefixFromStockKey(stockKey);

        unlinkByPattern(eventPrefix + ":user:*");
        unlinkByPattern(eventPrefix + ":status:*");
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock));

        return AdminTicketInitResponseDTO.builder()
                .eventId(eventId)
                .stock(stock)
                .build();
    }

    private String eventPrefixFromStockKey(String stockKey) {
        if (!stockKey.endsWith(STOCK_SUFFIX)) {
            throw new IllegalStateException("stock key must end with :stock");
        }
        return stockKey.substring(0, stockKey.length() - STOCK_SUFFIX.length());
    }

    private void unlinkByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_COUNT)
                .build();

        List<String> batch = new ArrayList<>(UNLINK_BATCH_SIZE);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= UNLINK_BATCH_SIZE) {
                    unlinkBatch(batch);
                }
            }
            unlinkBatch(batch);
        }
    }

    private void unlinkBatch(List<String> batch) {
        if (batch.isEmpty()) {
            return;
        }
        redisTemplate.unlink(batch);
        batch.clear();
    }
}
